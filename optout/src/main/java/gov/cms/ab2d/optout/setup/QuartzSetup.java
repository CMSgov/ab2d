package gov.cms.ab2d.optout.setup;

import gov.cms.ab2d.optout.OptoutJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.optout.properties")
public class QuartzSetup {

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
                .withSchedule(SimpleScheduleBuilder.repeatHourlyForever())
                .build();
    }


}
