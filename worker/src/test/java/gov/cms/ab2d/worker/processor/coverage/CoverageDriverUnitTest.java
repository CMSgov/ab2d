package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoverageJobStatus;
import gov.cms.ab2d.coverage.model.CoverageMapping;
import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearch;
import gov.cms.ab2d.coverage.model.CoverageSearchEvent;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.common.PropertyServiceStub;
import gov.cms.ab2d.worker.config.ContractToContractCoverageMapping;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import javax.persistence.EntityNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;


import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static gov.cms.ab2d.common.util.PropertyConstants.COVERAGE_SEARCH_OVERRIDE;
import static gov.cms.ab2d.common.util.PropertyConstants.COVERAGE_SEARCH_STUCK_HOURS;
import static gov.cms.ab2d.common.util.PropertyConstants.COVERAGE_SEARCH_UPDATE_MONTHS;
import static gov.cms.ab2d.common.util.PropertyConstants.MAINTENANCE_MODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Tests for paging coverage which are much easier using mocked resources
 */
@ExtendWith(MockitoExtension.class)
class CoverageDriverUnitTest {

    @Mock
    private CoverageService coverageService;

    @Mock
    private CoverageLockWrapper lockWrapper;

    @Mock
    private CoverageProcessor coverageProcessor;

    private PropertiesService propertiesService = new PropertyServiceStub();

    @Mock
    private ContractToContractCoverageMapping mapping;

    private final Lock tryLockFalse = new Lock() {
        @Override
        public void lock() {

        }

        @Override
        public void lockInterruptibly() throws InterruptedException {

        }

        @Override
        public boolean tryLock() {
            return false;
        }

        @Override
        public boolean tryLock(long time, @NotNull TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public void unlock() {

        }

        @NotNull
        @Override
        public Condition newCondition() {
            return null;
        }
    };

    private final Lock tryLockInterrupt = new Lock() {
        @Override
        public void lock() {

        }

        @Override
        public void lockInterruptibly() throws InterruptedException {

        }

        @Override
        public boolean tryLock() {
            return false;
        }

        @Override
        public boolean tryLock(long time, @NotNull TimeUnit unit) throws InterruptedException {
            throw new InterruptedException("this is a test");
        }

        @Override
        public void unlock() {

        }

        @NotNull
        @Override
        public Condition newCondition() {
            return null;
        }
    };

    private CoverageDriverImpl driver;

    @BeforeEach
    void before() {
        driver = new CoverageDriverImpl(null, null, coverageService, null, null, null,mapping);
    }

    @AfterEach
    void after() {
        reset(coverageService, lockWrapper, coverageProcessor);
        ((PropertyServiceStub) propertiesService).reset();
    }

    @DisplayName("Paging coverage fails when ")
    @Test
    void failPagingRequestWhenContractMissing() {

        CoverageDriverException contractMissing = assertThrows(CoverageDriverException.class, () -> driver.pageCoverage(new Job(), null));

        assertEquals("cannot retrieve metadata for job missing contract", contractMissing.getMessage());
    }

    @DisplayName("Paging coverage fails when start date is in future")
    @Test
    void failPagingRequestWhenStartDateAfterNow() {

        Job job = new Job();
        ContractDTO contract = new ContractDTO(null, null, OffsetDateTime.now().plusHours(1), null);

        CoverageDriverException startDateInFuture = assertThrows(CoverageDriverException.class, () -> driver.pageCoverage(job, contract));
        assertEquals("contract attestation time is after current time," +
                " cannot find metadata for coverage periods in the future", startDateInFuture.getMessage());
    }

    @DisplayName("Paging coverage ignores since date in future and executes search")
    @Test
    void pageRequestWhenSinceDateAfterNow() {

        when(coverageService.getCoveragePeriod(any(ContractForCoverageDTO.class), anyInt(), anyInt())).thenAnswer((invocationOnMock) -> {
            CoveragePeriod period = new CoveragePeriod();
            period.setContractNumber(invocationOnMock.getArgument(0).toString());
            period.setMonth(invocationOnMock.getArgument(1));
            period.setYear(invocationOnMock.getArgument(2));

            return period;
        });

        int pagingSize = (int) ReflectionTestUtils.getField(driver, "PAGING_SIZE");

        when(coverageService.pageCoverage(any(CoveragePagingRequest.class))).thenAnswer((invocationMock) -> {
            CoveragePagingRequest request = invocationMock.getArgument(0);

            Optional<Long> cursor = request.getCursor();

            CoveragePagingRequest nextRequest = null;
            if (cursor.isPresent()) {
                long cursorValue = cursor.get();
                nextRequest = new CoveragePagingRequest(pagingSize, (cursorValue + pagingSize), request.getContract(), request.getJobStartTime());
            } else {
                nextRequest = new CoveragePagingRequest(pagingSize, (long) pagingSize, request.getContract(), request.getJobStartTime());

            }

            return new CoveragePagingResult(List.of(), nextRequest);
        });

        Job job = new Job();
        ContractDTO contract =new ContractDTO("contract-0", null, OffsetDateTime.now().plusHours(1), null);
        when(mapping.map(any(ContractDTO.class))).thenReturn(new ContractForCoverageDTO(contract.getContractNumber(), contract.getAttestedOn(), ContractForCoverageDTO.ContractType.NORMAL));


        CoverageDriverException startDateInFuture = assertThrows(CoverageDriverException.class, () -> driver.pageCoverage(job, contract));
        assertEquals("contract attestation time is after current time," +
                " cannot find metadata for coverage periods in the future", startDateInFuture.getMessage());

        ContractDTO secondContract =new ContractDTO("contract-0", null, AB2D_EPOCH.toOffsetDateTime(), null);

        job.setSince(OffsetDateTime.now().plusHours(1));

        CoveragePagingResult result = driver.pageCoverage(job, secondContract);
        assertNotNull(result);
    }

    @DisplayName("Paging coverage fails when coverage periods are missing")
    @Test
    void failPagingWhenCoveragePeriodMissing() {

        when(coverageService.getCoveragePeriod(any(), anyInt(), anyInt())).thenThrow(new EntityNotFoundException());

        Job job = new Job();
        ContractDTO contract =new ContractDTO(null, null, AB2D_EPOCH.toOffsetDateTime(), null);

        CoverageDriverException startDateInFuture = assertThrows(CoverageDriverException.class, () -> driver.pageCoverage(job, contract));
        assertEquals(EntityNotFoundException.class, startDateInFuture.getCause().getClass());
    }

    @DisplayName("Paging coverage periods")
    @Test
    void beginPagingWhenCoveragePeriodsPresent() {

        when(coverageService.getCoveragePeriod(any(ContractForCoverageDTO.class), anyInt(), anyInt())).thenAnswer((invocationOnMock) -> {
            CoveragePeriod period = new CoveragePeriod();
            period.setContractNumber((invocationOnMock.getArgument(0).toString()));
            period.setMonth(invocationOnMock.getArgument(1));
            period.setYear(invocationOnMock.getArgument(2));

            return period;
        });

        int pagingSize = (int) ReflectionTestUtils.getField(driver, "PAGING_SIZE");

        when(coverageService.pageCoverage(any(CoveragePagingRequest.class))).thenAnswer((invocationMock) -> {
            CoveragePagingRequest request = invocationMock.getArgument(0);

            Optional<Long> cursor = request.getCursor();

            CoveragePagingRequest nextRequest = null;
            if (cursor.isPresent()) {
                long cursorValue = cursor.get();
                nextRequest = new CoveragePagingRequest(pagingSize, (cursorValue + pagingSize), request.getContract(), request.getJobStartTime());
            } else {
                nextRequest = new CoveragePagingRequest(pagingSize, (long) pagingSize, request.getContract(), request.getJobStartTime());

            }

            return new CoveragePagingResult(List.of(), nextRequest);
        });

        Job job = new Job();
        ContractDTO contract = new ContractDTO("Contract-0", null, AB2D_EPOCH.toOffsetDateTime(), null);


        when(mapping.map(any(ContractDTO.class))).thenReturn(new ContractForCoverageDTO("Contract-0", contract.getAttestedOn(), ContractForCoverageDTO.ContractType.NORMAL));


        CoveragePagingResult firstCall = driver.pageCoverage(job, contract);
        assertNotNull(firstCall);
        assertTrue(firstCall.getNextRequest().isPresent());

        CoveragePagingRequest firstNextRequest = firstCall.getNextRequest().get();
        assertTrue(firstNextRequest.getCursor().isPresent());
        assertEquals(pagingSize, firstNextRequest.getCursor().get());

        CoveragePagingResult secondCall = driver.pageCoverage(firstNextRequest);
        assertNotNull(secondCall);
        assertTrue(secondCall.getNextRequest().isPresent());

        CoveragePagingRequest secondNextRequest = secondCall.getNextRequest().get();
        assertTrue(secondNextRequest.getCursor().isPresent());
        assertEquals((2L * pagingSize), secondNextRequest.getCursor().get());
    }

    @DisplayName("When locking fails throws exceptions")
    @Test
    void failureToLockCausesExceptions() {

        when(lockWrapper.getCoverageLock()).thenReturn(tryLockFalse);

        CoverageDriver driver = new CoverageDriverImpl(null, null, coverageService, propertiesService, null, lockWrapper,null);

        CoverageDriverException exception = assertThrows(CoverageDriverException.class, driver::discoverCoveragePeriods);
        assertTrue(exception.getMessage().contains("could not retrieve lock"));

        exception = assertThrows(CoverageDriverException.class, driver::queueStaleCoveragePeriods);
        assertTrue(exception.getMessage().contains("could not retrieve lock"));
    }

    @DisplayName("When locking is interrupted propagate exception")
    @Test
    void whenLockInterruptedPropagateException() {

        when(lockWrapper.getCoverageLock()).thenReturn(tryLockInterrupt);

        ContractDTO contract = new ContractDTO("contractNum", null, null, null);

        Job job = new Job();
        job.setContractNumber(contract.getContractNumber());

        CoverageDriver driver = new CoverageDriverImpl(null, null, coverageService, propertiesService, null, lockWrapper,null);

        assertThrows(InterruptedException.class, driver::discoverCoveragePeriods);
        assertThrows(InterruptedException.class, driver::queueStaleCoveragePeriods);
        assertThrows(InterruptedException.class, () -> driver.isCoverageAvailable(job, contract));
    }

    @DisplayName("When locking fails return false for coverage available")
    @Test
    void failureToLockCoverageAvailableFailsQuietly() {
        when(lockWrapper.getCoverageLock()).thenReturn(tryLockFalse);
        doReturn(Collections.emptyList()).when(coverageService).coveragePeriodNeverSearchedSuccessfully();
        when(coverageService.coveragePeriodStuckJobs(any())).thenReturn(Collections.emptyList());
        when(coverageService.coveragePeriodNotUpdatedSince(anyInt(), anyInt(), any())).thenReturn(Collections.emptyList());

        CoverageDriver driver = new CoverageDriverImpl(null, null, coverageService, null, null, lockWrapper,null);

        ContractDTO contract = new ContractDTO("contractNum", null, null, null);
        Job job = new Job();
        job.setContractNumber(contract.getContractNumber());

        try {
            assertFalse(driver.isCoverageAvailable(job, contract));
        } catch (InterruptedException interruptedException) {
            fail("test interrupted during execution");
        }
    }

    @DisplayName("When paging coverage fails throw coverage driver exception")
    @Test
    void failureToPageCausesExceptions() {
        when(coverageService.pageCoverage(any())).thenThrow(RuntimeException.class);

        CoverageDriver driver = new CoverageDriverImpl(null, null, coverageService, null, null, null,null);

        ContractForCoverageDTO contract = new ContractForCoverageDTO();
        contract.setContractNumber("contractNum");

        CoverageDriverException exception = assertThrows(CoverageDriverException.class, () -> driver.pageCoverage(new CoveragePagingRequest( 1000, null, contract, OffsetDateTime.now())));
        assertTrue(exception.getMessage().contains("coverage driver failing preconditions"));
    }

    @DisplayName("When loading a mapping job exit early if conditions not met")
    @Test
    void loadMappingFailsQuietly() {
        CoverageDriverImpl driver = spy(new CoverageDriverImpl(null, null,
                coverageService, propertiesService, coverageProcessor, lockWrapper,null)
        );

        propertiesService.updateProperty(MAINTENANCE_MODE, "true");
        try {
            driver.loadMappingJob();
        } catch (Exception exception) {
            fail("maintenance mode should cause job to fail quietly", exception);
        }

        propertiesService.updateProperty(MAINTENANCE_MODE, "false");
        doReturn(true).when(coverageProcessor).isProcessorBusy();
        try {
            driver.loadMappingJob();
        } catch (Exception exception) {
            fail("coverage processor busy should cause job to fail quietly", exception);
        }

        doReturn(false).when(coverageProcessor).isProcessorBusy();
        doReturn(tryLockFalse).when(lockWrapper).getCoverageLock();
        try {
            driver.loadMappingJob();
        } catch (Exception exception) {
            fail("no search found should fail quietly", exception);
        }

        ContractDTO contract = new ContractDTO("contractNum", null, null, null);
        CoveragePeriod coveragePeriod = new CoveragePeriod();
        coveragePeriod.setId(100);
        coveragePeriod.setMonth(1);
        coveragePeriod.setYear(2021);
        coveragePeriod.setContractNumber(contract.getContractNumber());

        CoverageSearchEvent event = new CoverageSearchEvent();
        event.setCoveragePeriod(coveragePeriod);

        CoverageSearch search = new CoverageSearch();

        CoverageMapping mapping = new CoverageMapping(event, search);

        doReturn(Optional.of(search)).when(driver).getNextSearch();
        doReturn(Optional.empty()).when(coverageService).startSearch(any(), anyString());
        try {
            driver.loadMappingJob();
        } catch (Exception exception) {
            fail("coverage service not starting search should not fail", exception);
        }

        doReturn(Optional.of(mapping)).when(coverageService).startSearch(any(), anyString());
        doReturn(false).when(coverageProcessor).startJob(any());
        doReturn(event).when(coverageService).cancelSearch(anyInt(), anyString());
        doNothing().when(coverageProcessor).queueMapping(any(), anyBoolean());
        try {
            driver.loadMappingJob();
        } catch (Exception exception) {
            fail("coverage processor failing to start jobs should fail quietly", exception);
        }
    }

    @DisplayName("Coverage period update fails then throw exception")
    @Test
    void startDateForcedToMinAB2DEpoch() {

        ContractDTO contract = new ContractDTO("contractNum", null, OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), null);
        CoveragePeriod coveragePeriod = new CoveragePeriod();
        coveragePeriod.setId(100);
        coveragePeriod.setMonth(1);
        coveragePeriod.setYear(2021);
        coveragePeriod.setContractNumber(contract.getContractNumber());

        ZonedDateTime dateTime = driver.getStartDateTime(contract);
        assertEquals(AB2D_EPOCH, dateTime);
    }

    @DisplayName("Coverage period update fails then throw exception")
    @Test
    void periodUpdateFailsThenThrowException() {

        CoverageDriverException exception = assertThrows(CoverageDriverException.class, () -> {
            ContractDTO contract = new ContractDTO("contractNum", null, null, null);

            CoveragePeriod coveragePeriod = new CoveragePeriod();
            coveragePeriod.setContractNumber(contract.getContractNumber());
            coveragePeriod.setModified(OffsetDateTime.now().plus(1, ChronoUnit.HOURS));
            coveragePeriod.setStatus(CoverageJobStatus.FAILED);

            Job job = new Job();
            job.setCreatedAt(OffsetDateTime.now());

            driver.checkCoveragePeriodValidity(job, coveragePeriod);
        });

        assertTrue(exception.getMessage().contains("attempts to pull coverage information"));
    }
}
