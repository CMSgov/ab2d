package gov.cms.ab2d.worker.quartz;

import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.properties.PropertyServiceStub;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriverStub;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionException;


import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;
import static gov.cms.ab2d.common.util.PropertyConstants.COVERAGE_SEARCH_DISCOVERY;
import static gov.cms.ab2d.common.util.PropertyConstants.COVERAGE_SEARCH_OVERRIDE;
import static gov.cms.ab2d.common.util.PropertyConstants.COVERAGE_SEARCH_QUEUEING;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CoveragePeriodQuartzJobTest {

    private PropertiesService propertiesService = new PropertyServiceStub();

    @Mock
    private CoverageDriver coverageDriverMock;

    @Mock
    private SQSEventClient logManager;

    @AfterEach
    void tearDown() {
        ((PropertyServiceStub) propertiesService).reset();
        reset(logManager);
    }

    @DisplayName("Disengaging coverage search works")
    @Test
    void disengageSearchWorks() {

        propertiesService.updateProperty(COVERAGE_SEARCH_DISCOVERY, "idle");
        propertiesService.updateProperty(COVERAGE_SEARCH_QUEUEING, "idle");
        propertiesService.updateProperty(COVERAGE_SEARCH_OVERRIDE, "true");

        CoverageDriverStub coverageDriverStub = new CoverageDriverStub();

        CoveragePeriodQuartzJob quartzJob = new CoveragePeriodQuartzJob(coverageDriverStub, propertiesService, logManager);

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

        PropertiesService propertiesService = new PropertyServiceStub();

        propertiesService.updateProperty(COVERAGE_SEARCH_DISCOVERY, "engaged");
        propertiesService.updateProperty(COVERAGE_SEARCH_QUEUEING, "engaged");
        propertiesService.updateProperty(COVERAGE_SEARCH_OVERRIDE, "true");

        CoverageDriverStub coverageDriverStub = new CoverageDriverStub();
        CoveragePeriodQuartzJob quartzJob = new CoveragePeriodQuartzJob(coverageDriverStub, propertiesService, logManager);

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

        PropertiesService propertiesService = new PropertyServiceStub();

        propertiesService.updateProperty(COVERAGE_SEARCH_DISCOVERY, "engaged");
        propertiesService.updateProperty(COVERAGE_SEARCH_QUEUEING, "engaged");
        propertiesService.updateProperty(COVERAGE_SEARCH_OVERRIDE, "false");

        CoverageDriverStub coverageDriverStub = new CoverageDriverStub();
        CoveragePeriodQuartzJob quartzJob = new CoveragePeriodQuartzJob(coverageDriverStub, propertiesService, logManager);

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

        PropertiesService propertiesService = new PropertyServiceStub();

        propertiesService.updateProperty(COVERAGE_SEARCH_DISCOVERY, "engaged");
        propertiesService.updateProperty(COVERAGE_SEARCH_OVERRIDE, "true");

        try {
            doThrow(new RuntimeException("testing123")).when(coverageDriverMock).discoverCoveragePeriods();
            CoveragePeriodQuartzJob quartzJob = new CoveragePeriodQuartzJob(coverageDriverMock, propertiesService, logManager);

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
