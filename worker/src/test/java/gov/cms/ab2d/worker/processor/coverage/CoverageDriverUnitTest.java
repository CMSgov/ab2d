package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.coverage.model.*;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.EntityNotFoundException;
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

import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    @Mock
    private PropertiesService propertiesService;

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
        driver = new CoverageDriverImpl(null, null, coverageService, null, null, null);
    }

    @AfterEach
    void after() {
        reset(coverageService, lockWrapper, propertiesService, coverageProcessor);
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
        Contract contract = new Contract();
        contract.setAttestedOn(OffsetDateTime.now().plusHours(1));

        CoverageDriverException startDateInFuture = assertThrows(CoverageDriverException.class, () -> driver.pageCoverage(job, contract));
        assertEquals("contract attestation time is after current time," +
                " cannot find metadata for coverage periods in the future", startDateInFuture.getMessage());
    }

    @DisplayName("Paging coverage ignores since date in future and executes search")
    @Test
    void pageRequestWhenSinceDateAfterNow() {

        when(coverageService.getCoveragePeriod(any(Contract.class), anyInt(), anyInt())).thenAnswer((invocationOnMock) -> {
            CoveragePeriod period = new CoveragePeriod();
            period.setContract(invocationOnMock.getArgument(0));
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
        Contract contract = new Contract();
        contract.setAttestedOn(OffsetDateTime.now().plusHours(1));

        CoverageDriverException startDateInFuture = assertThrows(CoverageDriverException.class, () -> driver.pageCoverage(job, contract));
        assertEquals("contract attestation time is after current time," +
                " cannot find metadata for coverage periods in the future", startDateInFuture.getMessage());

        contract.setAttestedOn(AB2D_EPOCH.toOffsetDateTime());
        job.setSince(OffsetDateTime.now().plusHours(1));

        CoveragePagingResult result = driver.pageCoverage(job, contract);
        assertNotNull(result);
    }

    @DisplayName("Paging coverage fails when coverage periods are missing")
    @Test
    void failPagingWhenCoveragePeriodMissing() {

        when(coverageService.getCoveragePeriod(any(), anyInt(), anyInt())).thenThrow(new EntityNotFoundException());

        Job job = new Job();
        Contract contract = new Contract();
        contract.setAttestedOn(AB2D_EPOCH.toOffsetDateTime());

        CoverageDriverException startDateInFuture = assertThrows(CoverageDriverException.class, () -> driver.pageCoverage(job, contract));
        assertEquals(EntityNotFoundException.class, startDateInFuture.getCause().getClass());
    }

    @DisplayName("Paging coverage periods")
    @Test
    void beginPagingWhenCoveragePeriodsPresent() {

        when(coverageService.getCoveragePeriod(any(Contract.class), anyInt(), anyInt())).thenAnswer((invocationOnMock) -> {
            CoveragePeriod period = new CoveragePeriod();
            period.setContract(invocationOnMock.getArgument(0));
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
        Contract contract = new Contract();
        contract.setAttestedOn(AB2D_EPOCH.toOffsetDateTime());

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

        Properties monthsProp = new Properties();
        monthsProp.setValue("3");
        when(propertiesService.getPropertiesByKey(eq(Constants.COVERAGE_SEARCH_UPDATE_MONTHS)))
                .thenReturn(monthsProp);

        Properties stuckProp = new Properties();
        stuckProp.setValue("72");
        when(propertiesService.getPropertiesByKey(eq(Constants.COVERAGE_SEARCH_STUCK_HOURS)))
                .thenReturn(stuckProp);

        Properties overrideProp = new Properties();
        overrideProp.setValue("false");
        when(propertiesService.getPropertiesByKey(eq(Constants.COVERAGE_SEARCH_OVERRIDE)))
                .thenReturn(overrideProp);

        CoverageDriver driver = new CoverageDriverImpl(null, null, coverageService, propertiesService, null, lockWrapper);

        CoverageDriverException exception = assertThrows(CoverageDriverException.class, driver::discoverCoveragePeriods);
        assertTrue(exception.getMessage().contains("could not retrieve lock"));

        exception = assertThrows(CoverageDriverException.class, driver::queueStaleCoveragePeriods);
        assertTrue(exception.getMessage().contains("could not retrieve lock"));


    }

    @DisplayName("When locking is interrupted propagate exception")
    @Test
    void whenLockInterruptedPropagateException() {

        when(lockWrapper.getCoverageLock()).thenReturn(tryLockInterrupt);

        Properties monthsProp = new Properties();
        monthsProp.setValue("3");
        when(propertiesService.getPropertiesByKey(eq(Constants.COVERAGE_SEARCH_UPDATE_MONTHS)))
                .thenReturn(monthsProp);

        Properties stuckProp = new Properties();
        stuckProp.setValue("72");
        when(propertiesService.getPropertiesByKey(eq(Constants.COVERAGE_SEARCH_STUCK_HOURS)))
                .thenReturn(stuckProp);

        Properties overrideProp = new Properties();
        overrideProp.setValue("false");
        when(propertiesService.getPropertiesByKey(eq(Constants.COVERAGE_SEARCH_OVERRIDE)))
                .thenReturn(overrideProp);

        Contract contract = new Contract();
        contract.setContractNumber("contractNum");
        Job job = new Job();
        job.setContractNumber(contract.getContractNumber());

        CoverageDriver driver = new CoverageDriverImpl(null, null, coverageService, propertiesService, null, lockWrapper);

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

        CoverageDriver driver = new CoverageDriverImpl(null, null, coverageService, null, null, lockWrapper);

        Contract contract = new Contract();
        contract.setContractNumber("contractNum");
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

        CoverageDriver driver = new CoverageDriverImpl(null, null, coverageService, null, null, null);

        Contract contract = new Contract();
        contract.setContractNumber("contractNum");

        CoverageDriverException exception = assertThrows(CoverageDriverException.class, () -> driver.pageCoverage(new CoveragePagingRequest( 1000, null, contract, OffsetDateTime.now())));
        assertTrue(exception.getMessage().contains("coverage driver failing preconditions"));
    }

    @DisplayName("When loading a mapping job exit early if conditions not met")
    @Test
    void loadMappingFailsQuietly() {


        CoverageDriverImpl driver = spy(new CoverageDriverImpl(null, null,
                coverageService, propertiesService, coverageProcessor, lockWrapper)
        );

        doReturn(true).when(propertiesService).isInMaintenanceMode();
        try {
            driver.loadMappingJob();
        } catch (Exception exception) {
            fail("maintenance mode should cause job to fail quietly", exception);
        }

        doReturn(false).when(propertiesService).isInMaintenanceMode();
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

        Contract contract = new Contract();
        contract.setContractNumber("contractNum");

        CoveragePeriod coveragePeriod = new CoveragePeriod();
        coveragePeriod.setId(100);
        coveragePeriod.setMonth(1);
        coveragePeriod.setYear(2021);
        coveragePeriod.setContract(contract);

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

        Contract contract = new Contract();
        contract.setContractNumber("contractNum");
        contract.setAttestedOn(OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));

        CoveragePeriod coveragePeriod = new CoveragePeriod();
        coveragePeriod.setId(100);
        coveragePeriod.setMonth(1);
        coveragePeriod.setYear(2021);
        coveragePeriod.setContract(contract);

        ZonedDateTime dateTime = driver.getStartDateTime(contract);
        assertEquals(AB2D_EPOCH, dateTime);
    }

    @DisplayName("Coverage period update fails then throw exception")
    @Test
    void periodUpdateFailsThenThrowException() {

        CoverageDriverException exception = assertThrows(CoverageDriverException.class, () -> {
            Contract contract = new Contract();
            contract.setContractNumber("contractNum");

            CoveragePeriod coveragePeriod = new CoveragePeriod();
            coveragePeriod.setContract(contract);
            coveragePeriod.setModified(OffsetDateTime.now().plus(1, ChronoUnit.HOURS));
            coveragePeriod.setStatus(JobStatus.FAILED);

            Job job = new Job();
            job.setCreatedAt(OffsetDateTime.now());

            driver.checkCoveragePeriodValidity(job, coveragePeriod);
        });

        assertTrue(exception.getMessage().contains("attempts to pull coverage information"));
    }
}
