package gov.cms.ab2d.worker.stuckjob;

//import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
//import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.audit.properties")
public class StuckJobQuartzSetup {

//    @Value("${cron.schedule}")
//    private String schedule;


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
                .withSchedule(SimpleScheduleBuilder.repeatMinutelyForTotalCount(25, 2))
//                .withSchedule(CronScheduleBuilder.cronSchedule(schedule))
                .build();
    }

}
