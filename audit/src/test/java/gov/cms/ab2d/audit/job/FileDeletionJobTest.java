package gov.cms.ab2d.audit.job;

import gov.cms.ab2d.audit.SpringBootApp;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.quartz.*;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static gov.cms.ab2d.audit.job.FileDeletionJob.AUDIT_FILES_TTL_HOURS;
import static gov.cms.ab2d.audit.job.FileDeletionJob.EFS_MOUNT;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
public class FileDeletionJobTest {

    @Value("${efs.mount}")
    private String efsMount;

    private static final String TEST_FILE = "testFile.txt";

    private static final String TEST_FILE_NOT_DELETED = "testFileNotDeleted.txt";

    private static final String TEST_FILE_NO_PERMISSIONS = "testFileNoPermissions.txt";

    private static final String TEST_DIRECTORY = "testDirectory";

    private static final String TEST_FILE_NESTED = TEST_DIRECTORY + "/testFileInDirectory.txt";

    // Change the creation time so that the file will be eligible for deletion
    private void changeFileCreationDate(Path path) throws IOException {
        BasicFileAttributeView attributes = Files.getFileAttributeView(path, BasicFileAttributeView.class);
        FileTime time = FileTime.fromMillis(LocalDate.now().minus(2, ChronoUnit.DAYS).toEpochDay());
        attributes.setTimes(time, time, time);
    }

    @Test
    public void checkToEnsureFilesDeleted() throws IOException, URISyntaxException {
        // Don't change the creation date on this file, but do so on the next 3
        Path destinationNotDeleted = Paths.get(efsMount, TEST_FILE_NOT_DELETED);
        URL urlNotDeletedFile = this.getClass().getResource("/" + TEST_FILE_NOT_DELETED);
        Path sourceNotDeleted = Paths.get(urlNotDeletedFile.toURI());
        Files.move(sourceNotDeleted, destinationNotDeleted, StandardCopyOption.REPLACE_EXISTING);

        Path destination = Paths.get(efsMount, TEST_FILE);
        URL url = this.getClass().getResource("/" + TEST_FILE);
        Path source = Paths.get(url.toURI());
        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(destination);

        Path destinationNoPermissions = Paths.get(efsMount, TEST_FILE_NO_PERMISSIONS);
        URL urlNoPermissions = this.getClass().getResource("/" + TEST_FILE_NO_PERMISSIONS);
        Path sourceNoPermissions = Paths.get(urlNoPermissions.toURI());
        Files.move(sourceNoPermissions, destinationNoPermissions, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(destinationNoPermissions);

        Runtime.getRuntime().exec("chflags uchg " + destinationNoPermissions);

        File dir = new File(efsMount + TEST_DIRECTORY);
        if (!dir.exists()) dir.mkdirs();

        Path nestedFileDestination = Paths.get(efsMount, TEST_FILE_NESTED);
        URL nestedFileUrl = this.getClass().getResource("/" + TEST_FILE_NESTED);
        Path nestedFileSource = Paths.get(nestedFileUrl.toURI());
        Files.move(nestedFileSource, nestedFileDestination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(nestedFileDestination);

        FileDeletionJob fileDeletionJob = new FileDeletionJob();
        Scheduler scheduler = Mockito.mock(Scheduler.class);
        TriggerFiredBundle triggerFiredBundle = Mockito.mock(TriggerFiredBundle.class);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(EFS_MOUNT, efsMount);
        jobDataMap.put(AUDIT_FILES_TTL_HOURS, 24);
        OperableTrigger operableTrigger = Mockito.mock(OperableTrigger.class);
        when(operableTrigger.getJobDataMap()).thenReturn(jobDataMap);
        when(triggerFiredBundle.getTrigger()).thenReturn(operableTrigger);
        JobDetail jobDetail = Mockito.mock(JobDetail.class);
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(triggerFiredBundle.getJobDetail()).thenReturn(jobDetail);
        JobExecutionContext jobExecutionContext = new JobExecutionContextImpl(scheduler, triggerFiredBundle, fileDeletionJob);

        fileDeletionJob.execute(jobExecutionContext);

        Assert.assertTrue(Files.notExists(destination));

        Assert.assertTrue(Files.notExists(nestedFileDestination));

        Assert.assertTrue(Files.exists(destinationNotDeleted));

        Assert.assertTrue(Files.exists(destinationNoPermissions));
    }
}
