package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.service.CoverageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Tests for paging coverage which are much easier using mocked resources
 */
@ExtendWith(MockitoExtension.class)
class CoverageDriverUnitTest {

    @Mock
    private CoverageService coverageService;

    private CoverageDriver driver;

    @BeforeEach
    void before() {
        driver = new CoverageDriverImpl(null, null, coverageService, null, null, null);
    }

    @DisplayName("Paging coverage fails when ")
    @Test
    void failPagingRequestWhenContractMissing() {

        CoverageDriverException contractMissing = assertThrows(CoverageDriverException.class, () -> driver.pageCoverage(new Job()));

        assertEquals("cannot retrieve metadata for job missing contract", contractMissing.getMessage());
    }

    @DisplayName("Paging coverage fails when start date is in future")
    @Test
    void failPagingRequestWhenStartDateAfterNow() {

        Job job = new Job();
        Contract contract = new Contract();
        contract.setAttestedOn(OffsetDateTime.now().plusHours(1));
        job.setContract(contract);

        CoverageDriverException startDateInFuture = assertThrows(CoverageDriverException.class, () -> driver.pageCoverage(job));
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

            Optional<String> cursor = request.getCursor();

            CoveragePagingRequest nextRequest = null;
            if (cursor.isPresent()) {
                int cursorValue = Integer.parseInt(cursor.get());
                nextRequest = new CoveragePagingRequest(pagingSize, "" + (cursorValue + pagingSize), List.of());
            } else {
                nextRequest = new CoveragePagingRequest(pagingSize, "" + pagingSize, List.of());

            }

            return new CoveragePagingResult(List.of(), nextRequest);
        });

        Job job = new Job();
        Contract contract = new Contract();
        contract.setAttestedOn(OffsetDateTime.now().plusHours(1));
        job.setContract(contract);

        CoverageDriverException startDateInFuture = assertThrows(CoverageDriverException.class, () -> driver.pageCoverage(job));
        assertEquals("contract attestation time is after current time," +
                " cannot find metadata for coverage periods in the future", startDateInFuture.getMessage());

        contract.setAttestedOn(AB2D_EPOCH.toOffsetDateTime());
        job.setSince(OffsetDateTime.now().plusHours(1));

        CoveragePagingResult result = driver.pageCoverage(job);
        assertNotNull(result);
    }

    @DisplayName("Paging coverage fails when coverage periods are missing")
    @Test
    void failPagingWhenCoveragePeriodMissing() {

        when(coverageService.getCoveragePeriod(any(), anyInt(), anyInt())).thenThrow(new EntityNotFoundException());

        Job job = new Job();
        Contract contract = new Contract();
        contract.setAttestedOn(AB2D_EPOCH.toOffsetDateTime());
        job.setContract(contract);

        CoverageDriverException startDateInFuture = assertThrows(CoverageDriverException.class, () -> driver.pageCoverage(job));
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

            Optional<String> cursor = request.getCursor();

            CoveragePagingRequest nextRequest = null;
            if (cursor.isPresent()) {
                int cursorValue = Integer.parseInt(cursor.get());
                nextRequest = new CoveragePagingRequest(pagingSize, "" + (cursorValue + pagingSize), List.of());
            } else {
                nextRequest = new CoveragePagingRequest(pagingSize, "" + pagingSize, List.of());

            }

            return new CoveragePagingResult(List.of(), nextRequest);
        });

        Job job = new Job();
        Contract contract = new Contract();
        contract.setAttestedOn(AB2D_EPOCH.toOffsetDateTime());
        job.setContract(contract);

        CoveragePagingResult firstCall = driver.pageCoverage(job);
        assertNotNull(firstCall);
        assertTrue(firstCall.getNextRequest().isPresent());

        CoveragePagingRequest firstNextRequest = firstCall.getNextRequest().get();
        assertTrue(firstNextRequest.getCursor().isPresent());
        assertEquals("" + pagingSize, firstNextRequest.getCursor().get());

        CoveragePagingResult secondCall = driver.pageCoverage(firstNextRequest);
        assertNotNull(secondCall);
        assertTrue(secondCall.getNextRequest().isPresent());

        CoveragePagingRequest secondNextRequest = secondCall.getNextRequest().get();
        assertTrue(secondNextRequest.getCursor().isPresent());
        assertEquals("" + (2 * pagingSize), secondNextRequest.getCursor().get());
    }
}
