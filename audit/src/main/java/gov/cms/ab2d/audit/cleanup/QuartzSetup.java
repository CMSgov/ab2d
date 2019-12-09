package gov.cms.ab2d.audit.cleanup;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.audit.properties")
public class QuartzSetup {

    @Value("${audit.files.cron}")
    private String schedule;

    @Bean
    JobDetail fileDeletionJobDetail() {
        return JobBuilder.newJob(FileDeletionJob.class)
                .withIdentity("fileDeletionJob")
                .storeDurably()
                .build();
    }

    @Bean
    Trigger fileDeletionJobTrigger(JobDetail optoutJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(optoutJobDetail)
                .withIdentity("fileDeletionTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(schedule))
                .build();
    }
}
