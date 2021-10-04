package gov.cms.ab2d.worker.quartz;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoveragePeriodQuartzConfig {

    private final String schedule;

    public CoveragePeriodQuartzConfig(@Value("${coverage.update.schedule}") String schedule) {
        this.schedule = schedule;
    }

    @Qualifier("coverage_period_update")
    @Bean
    JobDetail coveragePeriodJobDetail() {
        return JobBuilder.newJob(CoveragePeriodQuartzJob.class)
                .withIdentity("coverage_period_update")
                .storeDurably()
                .build();
    }

    @Bean
    Trigger coveragePeriodJobPeriodicTrigger(@Qualifier("coverage_period_update") JobDetail coveragePeriodJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(coveragePeriodJobDetail)
                .withIdentity("coverage_period_update_trigger_periodic")
                .withSchedule(CronScheduleBuilder.cronSchedule(schedule))
                .build();
    }
}
