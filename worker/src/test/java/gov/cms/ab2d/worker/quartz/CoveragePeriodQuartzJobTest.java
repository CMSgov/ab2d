package gov.cms.ab2d.worker.quartz;

import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriverStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionException;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;

import static gov.cms.ab2d.common.util.Constants.COVERAGE_SEARCH_OVERRIDE;
import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoveragePeriodQuartzJobTest {

    @DisplayName("Disengaging coverage search works")
    @Test
    void disengageSearchWorks() {

        PropertiesService propertiesService = mock(PropertiesService.class);

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

        CoverageDriverStub coverageProcessor = new CoverageDriverStub();

        CoveragePeriodQuartzJob quartzJob = new CoveragePeriodQuartzJob(coverageProcessor, propertiesService);

        try {
            quartzJob.executeInternal(null);

            assertFalse(coverageProcessor.discoveryCalled);
            assertFalse(coverageProcessor.queueingCalled);

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

        CoverageDriverStub coverageProcessor = new CoverageDriverStub();
        CoveragePeriodQuartzJob quartzJob = new CoveragePeriodQuartzJob(coverageProcessor, propertiesService);

        try {

            quartzJob.executeInternal(null);

            assertTrue(coverageProcessor.discoveryCalled);
            assertTrue(coverageProcessor.queueingCalled);

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

        CoverageDriverStub coverageProcessor = new CoverageDriverStub();
        CoveragePeriodQuartzJob quartzJob = new CoveragePeriodQuartzJob(coverageProcessor, propertiesService);

        try {

            quartzJob.executeInternal(null);

            assertTrue(coverageProcessor.discoveryCalled);


            OffsetDateTime date = OffsetDateTime.now(AB2D_ZONE);

            if (date.getDayOfWeek() == DayOfWeek.TUESDAY && date.getHour() == 0) {
                assertTrue(coverageProcessor.queueingCalled);
            } else {
                assertFalse(coverageProcessor.queueingCalled);
            }

        } catch (JobExecutionException jobExecutionException) {
            fail("could not execute normally", jobExecutionException);
        }
    }
}
