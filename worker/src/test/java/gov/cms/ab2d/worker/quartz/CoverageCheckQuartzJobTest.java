package gov.cms.ab2d.worker.quartz;

import gov.cms.ab2d.properties.service.PropertiesAPIService;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.processor.coverage.CoverageVerificationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CoverageCheckQuartzJobTest {

    @Mock
    private PropertiesAPIService propertiesApiService;

    @Mock
    private CoverageDriver coverageDriver;

    @Mock
    private LogManager logManager;

    @AfterEach
    void tearDown() {
        reset(propertiesApiService);
        reset(coverageDriver);
        reset(logManager);
    }

    @DisplayName("Skip job when in maintenance mode")
    @Test
    void whenMaintenanceMode_skipJob() {

        when(propertiesApiService.isInMaintenanceMode()).thenReturn(true);

        CoverageCheckQuartzJob job = new CoverageCheckQuartzJob(logManager, coverageDriver, propertiesApiService);

        try {
            job.executeInternal(null);
        } catch (JobExecutionException executionException) {
            fail("job should not execute in maintenance mode");
        }

        verify(propertiesApiService, times(1)).isInMaintenanceMode();
    }

    @DisplayName("Execute normally when not in maintenance mode")
    @Test
    void whenNotMaintenanceMode_executeJob() {

        when(propertiesApiService.isInMaintenanceMode()).thenReturn(false);

        CoverageCheckQuartzJob job = new CoverageCheckQuartzJob(logManager, coverageDriver, propertiesApiService);

        try {
            job.executeInternal(null);
        } catch (JobExecutionException executionException) {
            fail("job should execute successfully if no exception thrown");
        }

        verify(propertiesApiService, times(1)).isInMaintenanceMode();
        verify(coverageDriver, times(1)).verifyCoverage();
    }

    @DisplayName("Report issues when verification detects issues")
    @Test
    void whenVerificationException_alertWithIssues() {

        when(propertiesApiService.isInMaintenanceMode()).thenReturn(false);

        doThrow(new CoverageVerificationException("testing123", List.of("alertalert")))
                .when(coverageDriver).verifyCoverage();

        CoverageCheckQuartzJob job = new CoverageCheckQuartzJob(logManager, coverageDriver, propertiesApiService);

        JobExecutionException exception = assertThrows(JobExecutionException.class, () -> job.executeInternal(null));
        assertTrue(exception.getMessage().contains("testing123"));

        verify(propertiesApiService, times(1)).isInMaintenanceMode();
        verify(coverageDriver, times(1)).verifyCoverage();
        verify(logManager, times(1)).alert(
                argThat(alert -> alert.contains("alertalert") && alert.contains("Coverage verification failed")), anyList());
    }

    @DisplayName("Report failure to run verification as an alert")
    @Test
    void whenVerificationFailsToRun_alert() {

        when(propertiesApiService.isInMaintenanceMode()).thenReturn(false);

        doThrow(new RuntimeException("testing123"))
                .when(coverageDriver).verifyCoverage();

        CoverageCheckQuartzJob job = new CoverageCheckQuartzJob(logManager, coverageDriver, propertiesApiService);

        JobExecutionException exception = assertThrows(JobExecutionException.class, () -> job.executeInternal(null));
        assertTrue(exception.getMessage().contains("testing123"));

        verify(propertiesApiService, times(1)).isInMaintenanceMode();
        verify(coverageDriver, times(1)).verifyCoverage();
        verify(logManager, times(1)).alert(contains("could not verify coverage due"), anyList());
    }

}
