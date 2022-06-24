package gov.cms.ab2d.worker.quartz;

import gov.cms.ab2d.properties.service.PropertiesAPIService;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriverStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionException;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;

import static gov.cms.ab2d.common.util.PropertyConstants.COVERAGE_SEARCH_DISCOVERY;
import static gov.cms.ab2d.common.util.PropertyConstants.COVERAGE_SEARCH_OVERRIDE;
import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;
import static gov.cms.ab2d.common.util.PropertyConstants.COVERAGE_SEARCH_QUEUEING;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoveragePeriodQuartzJobTest {

    @Mock
    private PropertiesAPIService propertiesApiService;

    @Mock
    private CoverageDriver coverageDriverMock;

    @Mock
    private LogManager logManager;

    @AfterEach
    void tearDown() {
        reset(propertiesApiService);
        reset(logManager);
    }

    @DisplayName("Disengaging coverage search works")
    @Test
    void disengageSearchWorks() {

        when(propertiesApiService.getProperty(COVERAGE_SEARCH_DISCOVERY)).thenAnswer((arg) -> "idle");
        when(propertiesApiService.getProperty(COVERAGE_SEARCH_QUEUEING)).thenAnswer((arg) -> "idle");
        when(propertiesApiService.getProperty(COVERAGE_SEARCH_OVERRIDE)).thenAnswer((arg) -> "true");

        CoverageDriverStub coverageDriverStub = new CoverageDriverStub();

        CoveragePeriodQuartzJob quartzJob = new CoveragePeriodQuartzJob(coverageDriverStub, propertiesApiService, logManager);

        try {
            quartzJob.executeInternal(null);

            assertFalse(coverageDriverStub.discoveryCalled);
            assertFalse(coverageDriverStub.queueingCalled);

        } catch (JobExecutionException jobExecutionException) {
            // Exception never thrown by stubbed method
        }
    }

    @DisplayName("Engaging coverage search works")
    @Test
    void engageSearchWorks() {

        PropertiesAPIService propertiesApiService = mock(PropertiesAPIService.class);

        when(propertiesApiService.getProperty(COVERAGE_SEARCH_DISCOVERY)).thenAnswer((arg) -> "engaged");
        when(propertiesApiService.getProperty(COVERAGE_SEARCH_QUEUEING)).thenAnswer((arg) -> "engaged");
        when(propertiesApiService.getProperty(COVERAGE_SEARCH_OVERRIDE)).thenAnswer((arg) -> "true");

        CoverageDriverStub coverageDriverStub = new CoverageDriverStub();
        CoveragePeriodQuartzJob quartzJob = new CoveragePeriodQuartzJob(coverageDriverStub, propertiesApiService, logManager);

        try {
            quartzJob.executeInternal(null);

            assertTrue(coverageDriverStub.discoveryCalled);
            assertTrue(coverageDriverStub.queueingCalled);

        } catch (JobExecutionException jobExecutionException) {
            fail("could not execute normally", jobExecutionException);
        }
    }

    @DisplayName("Engaging coverage search works")
    @Test
    void engagedSearchNotTuesdayAtMidnightDoesNotFire() {

        PropertiesAPIService propertiesApiService = mock(PropertiesAPIService.class);

        when(propertiesApiService.getProperty(COVERAGE_SEARCH_DISCOVERY)).thenAnswer((arg) -> "engaged");
        when(propertiesApiService.getProperty(COVERAGE_SEARCH_QUEUEING)).thenAnswer((arg) -> "engaged");
        when(propertiesApiService.getProperty(COVERAGE_SEARCH_OVERRIDE)).thenAnswer((arg) -> "false");

        CoverageDriverStub coverageDriverStub = new CoverageDriverStub();
        CoveragePeriodQuartzJob quartzJob = new CoveragePeriodQuartzJob(coverageDriverStub, propertiesApiService, logManager);

        try {
            quartzJob.executeInternal(null);

            assertTrue(coverageDriverStub.discoveryCalled);

            OffsetDateTime date = OffsetDateTime.now(AB2D_ZONE);

            if (date.getDayOfWeek() == DayOfWeek.TUESDAY) {
                assertTrue(coverageDriverStub.queueingCalled);
            } else {
                assertFalse(coverageDriverStub.queueingCalled);
            }

        } catch (JobExecutionException jobExecutionException) {
            fail("could not execute normally", jobExecutionException);
        }
    }


    @DisplayName("Failing searches trigger alerts")
    @Test
    void failingSearchesTriggerAlerts() {

        PropertiesAPIService propertiesApiService = mock(PropertiesAPIService.class);

        when(propertiesApiService.getProperty(COVERAGE_SEARCH_DISCOVERY)).thenAnswer((arg) -> "engaged");
        when(propertiesApiService.getProperty(COVERAGE_SEARCH_OVERRIDE)).thenAnswer((arg) -> "true");
        try {
            doThrow(new RuntimeException("testing123")).when(coverageDriverMock).discoverCoveragePeriods();
            CoveragePeriodQuartzJob quartzJob = new CoveragePeriodQuartzJob(coverageDriverMock, propertiesApiService, logManager);

            JobExecutionException exception =
                    assertThrows(JobExecutionException.class, () -> quartzJob.executeInternal(null));

            assertTrue(exception.getMessage().contains("testing123"));

            verify(logManager, times(1))
                    .alert(contains("coverage period updates could not be conducted"), anyList());

        } catch (Exception ex) {
            fail("could not execute test due to interruption", ex);
        }
    }
}
