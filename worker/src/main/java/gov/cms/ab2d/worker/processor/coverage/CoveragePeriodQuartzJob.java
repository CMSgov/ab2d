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

    private final CoverageProcessor processor;
    private final PropertiesService propertiesService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        String engagement = propertiesService.getPropertiesByKey(Constants.COVERAGE_SEARCH_ENGAGEMENT).getValue();
        FeatureEngagement state = FeatureEngagement.fromString(engagement);

        if (state != FeatureEngagement.IN_GEAR) {
            log.warn("coverage search engagement is not engaged so skipping discovery and" +
                    " queueing of coverage searches");
            return;
        }

        log.info("running coverage period quartz job by first looking for new coverage periods and then queueing new" +
                " and stale coverage periods");

        try {
            processor.discoverCoveragePeriods();

            processor.queueStaleCoveragePeriods();
        } catch (Exception exception) {
            log.error("coverage period updates could not be conducted");
            throw new JobExecutionException(exception);
        }
    }
}
