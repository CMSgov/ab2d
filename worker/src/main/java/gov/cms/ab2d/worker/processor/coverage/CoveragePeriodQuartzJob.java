package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class CoveragePeriodQuartzJob extends QuartzJobBean {

    private final CoverageDriver driver;
    private final PropertiesService propertiesService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        log.info("running coverage period quartz job by first looking for new coverage periods and then queueing new" +
                " and stale coverage periods");

        try {

            String discoveryEngagement = propertiesService.getPropertiesByKey(Constants.COVERAGE_SEARCH_DISCOVERY).getValue();
            FeatureEngagement disvoeryState = FeatureEngagement.fromString(discoveryEngagement);

            if (disvoeryState == FeatureEngagement.IN_GEAR) {
                log.info("coverage search discovery is engaged so attempting to discover new coverage periods");
                driver.discoverCoveragePeriods();
            }

            String queueEngagement = propertiesService.getPropertiesByKey(Constants.COVERAGE_SEARCH_QUEUEING).getValue();
            FeatureEngagement queueState = FeatureEngagement.fromString(queueEngagement);

            if (queueState == FeatureEngagement.IN_GEAR) {
                log.info("coverage search queueing is engaged so attempting to queue searches for new coverage periods " +
                        "and stale coverage periods");
                driver.queueStaleCoveragePeriods();
            }
        } catch (Exception exception) {
            log.error("coverage period updates could not be conducted");
            throw new JobExecutionException(exception);
        }
    }
}
