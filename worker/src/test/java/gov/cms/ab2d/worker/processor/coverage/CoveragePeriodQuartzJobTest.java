package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoveragePeriodQuartzJobTest {

    @DisplayName("Disengaging coverage search works")
    @Test
    void disengageSearchWorks() {

        PropertiesService propertiesService = mock(PropertiesService.class);

        when(propertiesService.getPropertiesByKey(eq(Constants.COVERAGE_SEARCH_ENGAGEMENT))).thenAnswer((arg) -> {
            Properties engaged = new Properties();
            engaged.setKey(Constants.COVERAGE_SEARCH_ENGAGEMENT);
            engaged.setValue("idle");
            return engaged;
        });

        CoverageProcessorStub coverageProcessor = new CoverageProcessorStub();

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

        when(propertiesService.getPropertiesByKey(eq(Constants.COVERAGE_SEARCH_ENGAGEMENT))).thenAnswer((arg) -> {
            Properties engaged = new Properties();
            engaged.setKey(Constants.COVERAGE_SEARCH_ENGAGEMENT);
            engaged.setValue("engaged");
            return engaged;
        });

        CoverageProcessorStub coverageProcessor = new CoverageProcessorStub();
        CoveragePeriodQuartzJob quartzJob = new CoveragePeriodQuartzJob(coverageProcessor, propertiesService);

        try {

            quartzJob.executeInternal(null);

            assertTrue(coverageProcessor.discoveryCalled);
            assertTrue(coverageProcessor.queueingCalled);

        } catch (JobExecutionException jobExecutionException) {
            fail("could not execute normally", jobExecutionException);
        }
    }
}
