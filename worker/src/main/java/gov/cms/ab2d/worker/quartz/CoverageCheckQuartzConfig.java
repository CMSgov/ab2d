package gov.cms.ab2d.worker.quartz;

import gov.cms.ab2d.worker.quartz.v3.CoverageV3CheckQuartzJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Periodically verify that all business requirements related to the coverage/enrollment cached in the database
 *
 */
@Configuration
public class CoverageCheckQuartzConfig {

    // Quartz cron schedule for the job
    private final String schedule;

    public CoverageCheckQuartzConfig(@Value("${coverage.verify.schedule}") String schedule) {
        this.schedule = schedule;
    }

    @Qualifier("coverage_check")
    @Bean
    JobDetail coverageCheckJobDetail() {
        return JobBuilder.newJob(CoverageCheckQuartzJob.class)
                .withIdentity("coverage_check")
                .storeDurably()
                .build();
    }

    @Bean
    Trigger coverageCheckPeriodicJobTrigger(@Qualifier("coverage_check") JobDetail coverageCheckJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(coverageCheckJobDetail)
                .withIdentity("coverage_check_trigger_periodic")
                .withSchedule(CronScheduleBuilder.cronSchedule(schedule))
                .build();
    }

    @Qualifier("coverage_v3_check")
    @Bean
    JobDetail coverageV3CheckJobDetail() {
        return JobBuilder.newJob(CoverageV3CheckQuartzJob.class)
            .withIdentity("coverage_v3_check")
            .storeDurably()
            .build();
}

    @Bean
    Trigger coverageV3CheckPeriodicJobTrigger(@Qualifier("coverage_v3_check") JobDetail coverageCheckJobDetail) {
        return TriggerBuilder.newTrigger()
            .forJob(coverageCheckJobDetail)
            .withIdentity("coverage_v3_check_trigger_periodic")
            .withSchedule(CronScheduleBuilder.cronSchedule(schedule))
            .build();
    }
}
