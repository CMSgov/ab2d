package gov.cms.ab2d.worker.quartz;

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
}
