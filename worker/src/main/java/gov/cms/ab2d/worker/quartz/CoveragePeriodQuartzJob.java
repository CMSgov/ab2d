package gov.cms.ab2d.worker.quartz;

import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;


import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;
import static gov.cms.ab2d.common.util.PropertyConstants.COVERAGE_SEARCH_DISCOVERY;
import static gov.cms.ab2d.common.util.PropertyConstants.COVERAGE_SEARCH_OVERRIDE;
import static gov.cms.ab2d.common.util.PropertyConstants.COVERAGE_SEARCH_QUEUEING;
import static gov.cms.ab2d.eventclient.config.Ab2dEnvironment.PRODUCTION;
import static gov.cms.ab2d.eventclient.config.Ab2dEnvironment.SANDBOX;
import static gov.cms.ab2d.eventclient.events.SlackEvents.COVERAGE_UPDATES_FAILED;

/**
 * Periodically update enrollment cached in the database by pulling enrollment from BFD.
 *
 * Typically this runs every Tuesday at midnight eastern time; however, you can override the schedule if enrollment arrives
 * at an unexpected time by setting {@link gov.cms.ab2d.common.util.PropertyConstants#COVERAGE_SEARCH_OVERRIDE}.
 *
 * Outside of production this feature will be disabled in most environments. To configure to run weekly,
 * set the following properties in the database:
 *
 *      - {@link gov.cms.ab2d.common.util.PropertyConstants#COVERAGE_SEARCH_DISCOVERY} for all active contracts, find if those contracts are missing coverage periods
 *         and create those missing coverage periods
 *          and loaded for the first time
 *      - {@link gov.cms.ab2d.common.util.PropertyConstants#COVERAGE_SEARCH_QUEUEING} find all coverage periods missing enrollment or needing
 *          enrollment updated, trigger those updates
 *      - {@link gov.cms.ab2d.common.util.PropertyConstants#COVERAGE_SEARCH_OVERRIDE} normally this job only runs once a week, set this property to
 *          override that configuration and force an update to enrollment.
 *
 * This only needs to run as often as BFD receives updated enrollment.
 */
@Slf4j
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class CoveragePeriodQuartzJob extends QuartzJobBean {
    private final CoverageDriver driver;
    private final PropertiesService propertiesService;
    private final SQSEventClient logManager;
    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        boolean override = Boolean.parseBoolean(propertiesService.getProperty(COVERAGE_SEARCH_OVERRIDE, "false"));

        try {

            String discoveryEngagement = propertiesService.getProperty(COVERAGE_SEARCH_DISCOVERY, FeatureEngagement.IN_GEAR.getSerialValue());
            FeatureEngagement disvoeryState = FeatureEngagement.fromString(discoveryEngagement);

            if (disvoeryState == FeatureEngagement.IN_GEAR) {
                driver.discoverCoveragePeriods();
            } else {
                log.info("coverage search discovery is NOT engaged so can't discover new coverage periods");
            }

            String queueEngagement = propertiesService.getProperty(COVERAGE_SEARCH_QUEUEING, FeatureEngagement.IN_GEAR.getSerialValue());
            FeatureEngagement queueState = FeatureEngagement.fromString(queueEngagement);

            if (queueState == FeatureEngagement.IN_GEAR) {
                // Start this job every day on Tuesday at midnight
                // or override and force start
                OffsetDateTime now = OffsetDateTime.now(AB2D_ZONE);
                if ((now.getDayOfWeek() == DayOfWeek.TUESDAY) || override) {
                    driver.queueStaleCoveragePeriods();
                }

            } else {
                log.info("coverage search queueing is NOT engaged");
            }
        } catch (Exception exception) {
            log.error("coverage period updates could not be conducted");
            logManager.alert(COVERAGE_UPDATES_FAILED + " coverage period updates could not be conducted", List.of(PRODUCTION, SANDBOX));
            throw new JobExecutionException(exception);
        } finally {
            // Only use override once
            // override forces reload for all enabled contracts for last three months.
            // We don't want to be doing that over and over again.
            if (override) {
                propertiesService.updateProperty(COVERAGE_SEARCH_OVERRIDE, "false");
            }
        }
    }
}
