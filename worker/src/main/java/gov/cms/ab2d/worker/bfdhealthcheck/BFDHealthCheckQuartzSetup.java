package gov.cms.ab2d.worker.bfdhealthcheck;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "bfd.health.check.enabled", havingValue = "true", matchIfMissing = true)
public class BFDHealthCheckQuartzSetup {

    private final String schedule;

    public BFDHealthCheckQuartzSetup(@Value("${bfd.health.check.schedule}") String schedule) {
        this.schedule = schedule;
    }

    @Bean
    JobDetail bfdHealthCheckJobDetail() {
        return JobBuilder.newJob(BFDHealthCheckJob.class)
                .withIdentity("bfdhealthcheck_job")
                .storeDurably()
                .build();
    }

    @Bean
    Trigger bfdHealthCheckJobTrigger(JobDetail bfdHealthCheckJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(bfdHealthCheckJobDetail)
                .withIdentity("bfdhealthcheck_job_trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(schedule))
                .build();
    }
}
