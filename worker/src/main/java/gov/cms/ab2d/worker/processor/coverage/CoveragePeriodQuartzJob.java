package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;

import static gov.cms.ab2d.common.util.Constants.COVERAGE_SEARCH_OVERRIDE;
import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;

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
        boolean override = Boolean.parseBoolean(propertiesService
                .getPropertiesByKey(COVERAGE_SEARCH_OVERRIDE).getValue());

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

                // Start this job every day on Tuesday at midnight
                // or override and force start
                OffsetDateTime now = OffsetDateTime.now(AB2D_ZONE);
                if ((now.getDayOfWeek() == DayOfWeek.TUESDAY && now.getHour() == 0) || override) {  // NOPMD
                    driver.queueStaleCoveragePeriods();
                }

            }
        } catch (Exception exception) {
            log.error("coverage period updates could not be conducted");
            throw new JobExecutionException(exception);
        } finally {
            // Only use override once
            // override forces reload for all enabled contracts for last three months.
            // We don't want to be doing that over and over again.
            if (override) {
                PropertiesDTO overrideUpdate = new PropertiesDTO();
                overrideUpdate.setKey(COVERAGE_SEARCH_OVERRIDE);
                overrideUpdate.setValue("false");
            }
        }
    }
}
