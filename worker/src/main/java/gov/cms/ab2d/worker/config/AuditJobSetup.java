package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.audit.job.FileDeletionJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Slf4j
@Component
public class AuditJobSetup {

    @Value("${audit-files-cron}")
    private String auditFilesCron;

    @Value("${efs.mount}")
    private String efsMount;

    @Value("${audit-files-ttl-hours}")
    private int auditFilesTTLHours;

    public static final String JOB_NAME = "DeleteAuditFilesJob";

    public static final String CRON_GROUP = "FileDeletion";

    public static final String TRIGGER_NAME = "DeleteAuditFilesTrigger";

    @PostConstruct
    public void setupAuditJob() throws SchedulerException {
        SchedulerFactory sf = new StdSchedulerFactory();
        Scheduler scheduler = sf.getScheduler();

        JobDetail job = newJob(FileDeletionJob.class)
                .withIdentity(JOB_NAME, CRON_GROUP)
                .storeDurably()
                .build();
        job.getJobDataMap().put(FileDeletionJob.EFS_MOUNT, efsMount);
        job.getJobDataMap().put(FileDeletionJob.AUDIT_FILES_TTL_HOURS, auditFilesTTLHours);

        CronTrigger trigger = newTrigger()
                .withIdentity(TRIGGER_NAME, CRON_GROUP)
                .withSchedule(cronSchedule(auditFilesCron))
                .build();

        scheduler.scheduleJob(job, trigger);
        scheduler.start();

        log.info("Scheduler started with a cron of {}", auditFilesCron);
    }
}
