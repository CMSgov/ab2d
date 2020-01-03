package gov.cms.ab2d.worker.stuckjob;

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
public class StuckJobQuartzSetup {

    @Value("${stuck.job.cron.schedule}")
    private String stuckJobCronSchedule;


    @Bean
    JobDetail cancelStuckJobDetail() {
        return JobBuilder.newJob(CancelStuckJob.class)
                .withIdentity("cancel_stuck_job")
                .storeDurably()
                .build();
    }


    @Bean
    Trigger cancelStuckJobTrigger(JobDetail cancelStuckJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(cancelStuckJobDetail)
                .withIdentity("cancel_stuck_job_trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(stuckJobCronSchedule))
                .build();
    }

}
