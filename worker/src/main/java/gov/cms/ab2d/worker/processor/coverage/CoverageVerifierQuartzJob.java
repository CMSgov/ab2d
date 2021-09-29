package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.eventlogger.LogManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.List;

import static gov.cms.ab2d.eventlogger.Ab2dEnvironment.PRODUCTION;
import static gov.cms.ab2d.eventlogger.Ab2dEnvironment.SANDBOX;

@Slf4j
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class CoverageVerifierQuartzJob extends QuartzJobBean {

    private final LogManager logManager;
    private final CoverageDriver driver;
    private final PropertiesService propertiesService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        if (propertiesService.isInMaintenanceMode()) {
            log.info("Skipping enrollment verification because ");
        }

        try {
            driver.verifyCoverage();
        } catch (CoverageVerificationException exception) {
            log.error("coverage is invalid or not able to be verified", exception);

            logManager.alert("Verification failed:\n" + exception.getMessage(), List.of(SANDBOX, PRODUCTION));

            throw new JobExecutionException(exception);
        } catch (Exception exception) {
            log.error("unexpected failure attempting to verify coverage");

            logManager.alert("could not verify coverage due to " + exception.getClass()
                    + ": " + exception.getMessage(), List.of(SANDBOX, PRODUCTION));

            throw new JobExecutionException(exception);
        }
    }
}