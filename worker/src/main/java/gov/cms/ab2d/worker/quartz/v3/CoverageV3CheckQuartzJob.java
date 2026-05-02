package gov.cms.ab2d.worker.quartz.v3;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.processor.coverage.CoverageVerificationException;
import gov.cms.ab2d.worker.quartz.CoverageCheckQuartzJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.List;

import static gov.cms.ab2d.common.util.PropertyConstants.MAINTENANCE_MODE;
import static gov.cms.ab2d.eventclient.config.Ab2dEnvironment.PRODUCTION;
import static gov.cms.ab2d.eventclient.events.SlackEvents.COVERAGE_VERIFICATION_ABORTED;
import static gov.cms.ab2d.eventclient.events.SlackEvents.COVERAGE_VERIFICATION_FAILURE;


/**
 *
 * {@link CoverageCheckQuartzJob}
 */
@Slf4j
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class CoverageV3CheckQuartzJob extends QuartzJobBean {

    private final SQSEventClient logManager;
    private final CoverageDriver driver;
    private final PropertiesService propertiesService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        if (propertiesService.isToggleOn(MAINTENANCE_MODE, false)) {
            log.info("[V3] Skipping enrollment verification because AB2D is already in maintenance mode");
        }

//        try {
//            driver.verifyCoverageV3();
//        } catch (CoverageVerificationException exception) {
//            log.error("[V3] coverage is invalid or not able to be verified {}", exception.getAlertMessage());
//            logManager.alert("[V3] " + COVERAGE_VERIFICATION_FAILURE + " Coverage verification failed:\n" + exception.getAlertMessage(), List.of(PRODUCTION));
//
//            throw new JobExecutionException(exception);
//        } catch (Exception exception) {
//            log.error("[V3] Encountered unexpected failure attempting to verify coverage", exception);
//
//            logManager.alert("[V3] " + COVERAGE_VERIFICATION_ABORTED + " could not verify coverage due to " + exception.getClass()
//                    + ":\n" + exception.getMessage(), List.of(PRODUCTION));
//
//            throw new JobExecutionException(exception);
//        }
    }
}
