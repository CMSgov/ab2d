package gov.cms.ab2d.worker.quartz;

import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.Constants;
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

import static gov.cms.ab2d.common.util.Constants.COVERAGE_SEARCH_OVERRIDE;
import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoveragePeriodQuartzJobTest {

    @Mock
    private PropertiesService propertiesService;

    @Mock
    private CoverageDriver coverageDriverMock;

    @Mock
    private LogManager logManager;

    @AfterEach
    void tearDown() {
        reset(propertiesService);
        reset(logManager);
    }

    @DisplayName("Disengaging coverage search works")
    @Test
    void disengageSearchWorks() {

        when(propertiesService.getPropertiesByKey(eq(Constants.COVERAGE_SEARCH_DISCOVERY))).thenAnswer((arg) -> {
            Properties engaged = new Properties();
            engaged.setKey(Constants.COVERAGE_SEARCH_DISCOVERY);
            engaged.setValue("idle");
            return engaged;
        });

        when(propertiesService.getPropertiesByKey(eq(Constants.COVERAGE_SEARCH_QUEUEING))).thenAnswer((arg) -> {
            Properties engaged = new Properties();
            engaged.setKey(Constants.COVERAGE_SEARCH_QUEUEING);
            engaged.setValue("idle");
            return engaged;
        });

        when(propertiesService.getPropertiesByKey(eq(COVERAGE_SEARCH_OVERRIDE))).thenAnswer((arg) -> {
            Properties override = new Properties();
            override.setKey(COVERAGE_SEARCH_OVERRIDE);
            override.setValue("true");
            return override;
        });

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

        PropertiesService propertiesService = mock(PropertiesService.class);

        when(propertiesService.getPropertiesByKey(eq(Constants.COVERAGE_SEARCH_DISCOVERY))).thenAnswer((arg) -> {
            Properties engaged = new Properties();
            engaged.setKey(Constants.COVERAGE_SEARCH_DISCOVERY);
            engaged.setValue("engaged");
            return engaged;
        });

        when(propertiesService.getPropertiesByKey(eq(Constants.COVERAGE_SEARCH_QUEUEING))).thenAnswer((arg) -> {
            Properties engaged = new Properties();
            engaged.setKey(Constants.COVERAGE_SEARCH_QUEUEING);
            engaged.setValue("engaged");
            return engaged;
        });

        when(propertiesService.getPropertiesByKey(eq(COVERAGE_SEARCH_OVERRIDE))).thenAnswer((arg) -> {
            Properties override = new Properties();
            override.setKey(COVERAGE_SEARCH_OVERRIDE);
            override.setValue("true");
            return override;
        });

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

        PropertiesService propertiesService = mock(PropertiesService.class);

        when(propertiesService.getPropertiesByKey(eq(Constants.COVERAGE_SEARCH_DISCOVERY))).thenAnswer((arg) -> {
            Properties engaged = new Properties();
            engaged.setKey(Constants.COVERAGE_SEARCH_DISCOVERY);
            engaged.setValue("engaged");
            return engaged;
        });

        when(propertiesService.getPropertiesByKey(eq(Constants.COVERAGE_SEARCH_QUEUEING))).thenAnswer((arg) -> {
            Properties engaged = new Properties();
            engaged.setKey(Constants.COVERAGE_SEARCH_QUEUEING);
            engaged.setValue("engaged");
            return engaged;
        });

        when(propertiesService.getPropertiesByKey(eq(COVERAGE_SEARCH_OVERRIDE))).thenAnswer((arg) -> {
            Properties override = new Properties();
            override.setKey(COVERAGE_SEARCH_OVERRIDE);
            override.setValue("false");
            return override;
        });

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

        PropertiesService propertiesService = mock(PropertiesService.class);

        when(propertiesService.getPropertiesByKey(eq(Constants.COVERAGE_SEARCH_DISCOVERY))).thenAnswer((arg) -> {
            Properties engaged = new Properties();
            engaged.setKey(Constants.COVERAGE_SEARCH_DISCOVERY);
            engaged.setValue("engaged");
            return engaged;
        });

        when(propertiesService.getPropertiesByKey(eq(COVERAGE_SEARCH_OVERRIDE))).thenAnswer((arg) -> {
            Properties override = new Properties();
            override.setKey(COVERAGE_SEARCH_OVERRIDE);
            override.setValue("true");
            return override;
        });


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
