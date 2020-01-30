package gov.cms.ab2d.optout.setup;

import gov.cms.ab2d.optout.job.OptoutJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.optout.properties")
public class OptOutQuartzSetup {

    @Value("${cron.schedule}")
    private String schedule;

    @Bean
    JobDetail optoutJobDetail() {
        return JobBuilder.newJob(OptoutJob.class)
                .withIdentity("optout_job")
                .storeDurably()
                .build();
    }

    @Bean
    Trigger outputJobTrigger(JobDetail optoutJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(optoutJobDetail)
                .withIdentity("output_job_trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(schedule))
                .build();
    }
}
