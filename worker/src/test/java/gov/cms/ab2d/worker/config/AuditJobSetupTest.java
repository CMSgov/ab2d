package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.audit.job.FileDeletionJob;
import gov.cms.ab2d.worker.SpringBootApp;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static gov.cms.ab2d.worker.config.AuditJobSetup.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
public class AuditJobSetupTest {

    @Value("${efs.mount}")
    private String efsMount;

    @Value("${audit-files-ttl-hours}")
    private int auditFilesTTLHours;

    @Test
    public void testAuditJob() throws SchedulerException {
        SchedulerFactory sf = new StdSchedulerFactory();
        Scheduler scheduler = sf.getScheduler();

        JobKey jobKey = new JobKey(JOB_NAME, CRON_GROUP);

        boolean jobExists = scheduler.checkExists(jobKey);
        Assert.assertTrue(jobExists);

        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        String efsMountRetrieved = jobDetail.getJobDataMap().getString(FileDeletionJob.EFS_MOUNT);
        Assert.assertEquals(efsMount, efsMountRetrieved);
        int auditFilesTTLHoursRetrieved = jobDetail.getJobDataMap().getInt(FileDeletionJob.AUDIT_FILES_TTL_HOURS);
        Assert.assertEquals(auditFilesTTLHours, auditFilesTTLHoursRetrieved);

        boolean triggerExists = scheduler.checkExists(new TriggerKey(TRIGGER_NAME, CRON_GROUP));
        Assert.assertTrue(triggerExists);

        Assert.assertTrue(scheduler.isStarted());
    }
}
