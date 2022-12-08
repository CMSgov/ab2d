package gov.cms.ab2d.worker.quartz;

import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.properties.PropertyServiceStub;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.processor.coverage.CoverageVerificationException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionException;


import static gov.cms.ab2d.common.util.PropertyConstants.MAINTENANCE_MODE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CoverageCheckQuartzJobTest {

    private PropertiesService propertiesService = new PropertyServiceStub();

    @Mock
    private CoverageDriver coverageDriver;

    @Mock
    private SQSEventClient logManager;

    @AfterEach
    void tearDown() {
        ((PropertyServiceStub) propertiesService).reset();
        reset(coverageDriver);
        reset(logManager);
    }

    @DisplayName("Skip job when in maintenance mode")
    @Test
    void whenMaintenanceMode_skipJob() {

        propertiesService.updateProperty(MAINTENANCE_MODE, "true");

        CoverageCheckQuartzJob job = new CoverageCheckQuartzJob(logManager, coverageDriver, propertiesService);

        try {
            job.executeInternal(null);
        } catch (JobExecutionException executionException) {
            fail("job should not execute in maintenance mode");
        }
    }

    @DisplayName("Execute normally when not in maintenance mode")
    @Test
    void whenNotMaintenanceMode_executeJob() {

        propertiesService.updateProperty(MAINTENANCE_MODE, "false");

        CoverageCheckQuartzJob job = new CoverageCheckQuartzJob(logManager, coverageDriver, propertiesService);

        try {
            job.executeInternal(null);
        } catch (JobExecutionException executionException) {
            fail("job should execute successfully if no exception thrown");
        }

        verify(coverageDriver, times(1)).verifyCoverage();
    }

    @DisplayName("Report issues when verification detects issues")
    @Test
    void whenVerificationException_alertWithIssues() {

        propertiesService.updateProperty(MAINTENANCE_MODE, "false");

        doThrow(new CoverageVerificationException("testing123", List.of("alertalert")))
                .when(coverageDriver).verifyCoverage();

        CoverageCheckQuartzJob job = new CoverageCheckQuartzJob(logManager, coverageDriver, propertiesService);

        JobExecutionException exception = assertThrows(JobExecutionException.class, () -> job.executeInternal(null));
        assertTrue(exception.getMessage().contains("testing123"));

        verify(coverageDriver, times(1)).verifyCoverage();
        verify(logManager, times(1)).alert(
                argThat(alert -> alert.contains("alertalert") && alert.contains("Coverage verification failed")), anyList());
    }

    @DisplayName("Report failure to run verification as an alert")
    @Test
    void whenVerificationFailsToRun_alert() {

        propertiesService.updateProperty(MAINTENANCE_MODE, "false");

        doThrow(new RuntimeException("testing123"))
                .when(coverageDriver).verifyCoverage();

        CoverageCheckQuartzJob job = new CoverageCheckQuartzJob(logManager, coverageDriver, propertiesService);

        JobExecutionException exception = assertThrows(JobExecutionException.class, () -> job.executeInternal(null));
        assertTrue(exception.getMessage().contains("testing123"));

        verify(coverageDriver, times(1)).verifyCoverage();
        verify(logManager, times(1)).alert(contains("could not verify coverage due"), anyList());
    }

}
