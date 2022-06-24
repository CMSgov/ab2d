package gov.cms.ab2d.hpms.quartz;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 *  todo: Don't know how to make this nice feature work using with the PropertiesService.  Instead, just allow the timer
 *      to fire, invoking HPMSIngestJob which immediately returns if not engaged.
 *      @ConditionalOnProperty(name = PropertyConstants.HPMS_INGESTION_ENGAGEMENT, havingValue = "engaged", matchIfMissing = false)
 */
@Configuration
public class HPMSIngestQuartzSetup {

    @Value("${hpms.ingest.schedule}")
    private String schedule;

    @Bean
    JobDetail hpmsIngestJobDetail() {
        return JobBuilder.newJob(HPMSIngestJob.class)
                .withIdentity("hpms_ingest_job")
                .storeDurably()
                .build();
    }

    @Bean
    Trigger hpmsIngestJobTrigger(JobDetail bfdHealthCheckJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(bfdHealthCheckJobDetail)
                .withIdentity("hpms_ingest_job_trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(schedule))
                .build();
    }
}
