package gov.cms.ab2d.audit.cleanup;

import gov.cms.ab2d.audit.SpringBootApp;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.service.JobService;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.eventlogger.events.ContractBeneSearchEvent;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.eventlogger.events.JobStatusChangeEvent;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import gov.cms.ab2d.eventlogger.reports.sql.DoAll;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileSystemUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.audit.properties")
@Testcontainers
public class FileDeletionServiceTest {

    @TempDir
    File tmpDirFolder;

    @Autowired
    private FileDeletionService fileDeletionService;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private JobService jobService;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private DoAll doAll;

    private static final String TEST_FILE = "testFile.ndjson";

    private static final String TEST_FILE_NOT_DELETED = "testFileNotDeleted.ndjson";

    private static final String TEST_DIRECTORY_NO_PERMISSIONS = "testDirectoryNoPermissions";

    private static final String TEST_FILE_NO_PERMISSIONS = TEST_DIRECTORY_NO_PERMISSIONS + "/testFileNoPermissions.ndjson";

    private static final String TEST_DIRECTORY = "testDirectory";

    private static final String TEST_FILE_NESTED = TEST_DIRECTORY + "/testFileInDirectory.ndjson";

    private static final String REGULAR_FILE = "regularFile.txt";

    // Change the creation time so that the file will be eligible for deletion
    private void changeFileCreationDate(Path path) throws IOException {
        BasicFileAttributeView attributes = Files.getFileAttributeView(path, BasicFileAttributeView.class);
        FileTime time = FileTime.fromMillis(LocalDate.now().minus(2, ChronoUnit.DAYS).toEpochDay());
        attributes.setTimes(time, time, time);
    }

    private Job job;
    private Job jobInProgress;
    private Job jobNotExpiredYet;
    private String efsMount;

    @BeforeEach
    public void init() {
        doAll.delete();

        User user = dataSetup.setupUser(List.of());

        // Connected to a job that is finished and has expired
        job = new Job();
        job.setStatus(JobStatus.SUCCESSFUL);
        job.setJobUuid(UUID.randomUUID().toString());
        job.setCreatedAt(OffsetDateTime.now().minusDays(3));
        job.setCompletedAt(OffsetDateTime.now().minusDays(2));
        job.setExpiresAt(OffsetDateTime.now().minusDays(1));
        job.setUser(user);
        jobService.updateJob(job);

        // Connected to a job, but in progress
        jobInProgress = new Job();
        jobInProgress.setStatus(JobStatus.IN_PROGRESS);
        jobInProgress.setJobUuid(UUID.randomUUID().toString());
        jobInProgress.setCreatedAt(OffsetDateTime.now().minusHours(1));
        jobInProgress.setUser(user);
        jobService.updateJob(jobInProgress);

        // Connected to a job that is finished where the file has yet to expire
        jobNotExpiredYet = new Job();
        jobNotExpiredYet.setStatus(JobStatus.SUCCESSFUL);
        jobNotExpiredYet.setJobUuid(UUID.randomUUID().toString());
        jobNotExpiredYet.setCreatedAt(OffsetDateTime.now().minusHours(10));
        jobNotExpiredYet.setCompletedAt(OffsetDateTime.now().minusHours(5));
        jobNotExpiredYet.setExpiresAt(OffsetDateTime.now().plusHours(19));
        jobNotExpiredYet.setUser(user);

        efsMount = tmpDirFolder.toPath().toString();
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", efsMount);
    }

    @DisplayName("Delete unrelated top level ndjson file")
    @Test
    void deleteUnrelatedTopLevelNdjson() throws IOException, URISyntaxException {

        // Not connected to a job
        Path destination = Paths.get(efsMount, TEST_FILE);
        URL url = this.getClass().getResource("/" + TEST_FILE);
        Path source = Paths.get(url.toURI());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(destination);

        fileDeletionService.deleteFiles();

        assertTrue(Files.notExists(destination));

        List<LoggableEvent> fileEvents = doAll.load(FileEvent.class);
        FileEvent e1 = (FileEvent) fileEvents.get(0);
        assertTrue(e1.getFileName().equalsIgnoreCase(destination.toString()));

        checkNoOtherEventsLogged();
    }

    @DisplayName("Ignore unrelated ndjson file that was just created")
    @Test
    void ignoreNewlyCreatedNestedNdjson() throws IOException, URISyntaxException {
        // Don't change the creation date on this file, but do so on the next ones
        Path destinationNotDeleted = Paths.get(efsMount, TEST_FILE_NOT_DELETED);
        URL urlNotDeletedFile = this.getClass().getResource("/" + TEST_FILE_NOT_DELETED);
        Path sourceNotDeleted = Paths.get(urlNotDeletedFile.toURI());
        Files.copy(sourceNotDeleted, destinationNotDeleted, StandardCopyOption.REPLACE_EXISTING);

        fileDeletionService.deleteFiles();

        assertTrue(Files.exists(destinationNotDeleted));

        Files.delete(destinationNotDeleted);

        checkNoOtherEventsLogged();
    }

    @DisplayName("Delete nested ndjson file not attached to a job")
    @Test
    void deleteNestedNdjson() throws IOException, URISyntaxException {

        final Path dirPath = Paths.get(efsMount, TEST_DIRECTORY);
        File dir = new File(dirPath.toString());
        if (!dir.exists()) dir.mkdirs();

        // Not connected to a job
        Path nestedFileDestination = Paths.get(efsMount, TEST_FILE_NESTED);
        URL nestedFileUrl = this.getClass().getResource("/" + TEST_FILE_NESTED);
        Path nestedFileSource = Paths.get(nestedFileUrl.toURI());
        Files.copy(nestedFileSource, nestedFileDestination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(nestedFileDestination);

        fileDeletionService.deleteFiles();

        assertTrue(Files.notExists(nestedFileDestination));

        List<LoggableEvent> fileEvents = doAll.load(FileEvent.class);
        FileEvent e1 = (FileEvent) fileEvents.get(0);
        assertTrue(e1.getFileName().equalsIgnoreCase(nestedFileDestination.toString()));

        FileSystemUtils.deleteRecursively(dir);

        checkNoOtherEventsLogged();
    }

    @DisplayName("Ignore regular files without ndjson extension")
    @Test
    void ignoreRegularFiles() throws IOException, URISyntaxException {

        Path regularFileDestination = Paths.get(efsMount, REGULAR_FILE);
        URL regularFileUrl = this.getClass().getResource("/" + REGULAR_FILE);
        Path regularFileSource = Paths.get(regularFileUrl.toURI());
        Files.copy(regularFileSource, regularFileDestination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(regularFileDestination);

        fileDeletionService.deleteFiles();

        assertTrue(Files.exists(regularFileDestination));

        Files.delete(regularFileDestination);

        checkNoOtherEventsLogged();
    }

    @DisplayName("Ignore directory if no permissions")
    @Test
    void ignoreIfNoPermissions() throws IOException, URISyntaxException {
        // A directory with no permissions that isn't going to be deleted
        final Path noPermissionsDirPath = Paths.get(efsMount, TEST_DIRECTORY_NO_PERMISSIONS);
        File noPermissionsDir = new File(noPermissionsDirPath.toString());
        if (!noPermissionsDir.exists()) noPermissionsDir.mkdirs();

        Path noPermissionsFileDestination = Paths.get(efsMount, TEST_FILE_NO_PERMISSIONS);
        URL noPermissionsFileUrl = this.getClass().getResource("/" + TEST_FILE_NO_PERMISSIONS);
        Path noPermissionsFileSource = Paths.get(noPermissionsFileUrl.toURI());
        Files.copy(noPermissionsFileSource, noPermissionsFileDestination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(noPermissionsFileDestination);

        noPermissionsDir.setWritable(false);

        fileDeletionService.deleteFiles();

        assertTrue(Files.exists(noPermissionsFileDestination));

        noPermissionsDir.setWritable(true);
        FileSystemUtils.deleteRecursively(noPermissionsDir);

        checkNoOtherEventsLogged();
    }

    @Test
    void deleteCompletedAndExpiredJobFiles() throws IOException, URISyntaxException {

        Path jobPath = Paths.get(efsMount, job.getJobUuid());
        File jobDir = new File(jobPath.toString());
        if (!jobDir.exists()) jobDir.mkdirs();
        Path destinationJobConnection = Paths.get(jobPath.toString(), "S0000_0001.ndjson");
        URL urlJobConnection = this.getClass().getResource("/" + TEST_FILE);
        Path sourceJobConnection = Paths.get(urlJobConnection.toURI());
        Files.copy(sourceJobConnection, destinationJobConnection, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(destinationJobConnection);

        fileDeletionService.deleteFiles();

        assertTrue(Files.notExists(destinationJobConnection));

        List<LoggableEvent> fileEvents = doAll.load(FileEvent.class);
        FileEvent e1 = (FileEvent) fileEvents.get(0);
        assertTrue(e1.getFileName().equalsIgnoreCase(destinationJobConnection.toString()));

        checkNoOtherEventsLogged();
    }

    @DisplayName("Ignore in progress job files")
    @Test
    void ignoreInProgressJobFiles() throws IOException, URISyntaxException {

        Path jobInProgressPath = Paths.get(efsMount, jobInProgress.getJobUuid());
        File jobInProgressDir = new File(jobInProgressPath.toString());
        if (!jobInProgressDir.exists()) jobInProgressDir.mkdirs();
        Path destinationJobInProgressConnection = Paths.get(jobInProgressPath.toString(), "S0000_0001.ndjson");
        URL urlJobInProgressConnection = this.getClass().getResource("/" + TEST_FILE);
        Path sourceJobInProgressConnection = Paths.get(urlJobInProgressConnection.toURI());
        Files.copy(sourceJobInProgressConnection, destinationJobInProgressConnection, StandardCopyOption.REPLACE_EXISTING);

        assertTrue(Files.exists(destinationJobInProgressConnection));

        checkNoOtherEventsLogged();

        Files.delete(destinationJobInProgressConnection);
    }

    @DisplayName("Ignore recently completed job files")
    @Test
    void ignoreRecentlyCompletedJobFiles() throws IOException, URISyntaxException {

        jobService.updateJob(jobNotExpiredYet);
        Path jobNotExpiredYetPath = Paths.get(efsMount, jobNotExpiredYet.getJobUuid());
        File jobNotExpiredYetDir = new File(jobNotExpiredYetPath.toString());
        if (!jobNotExpiredYetDir.exists()) jobNotExpiredYetDir.mkdirs();
        Path destinationJobNotExpiredYetConnection = Paths.get(jobNotExpiredYetPath.toString(), "S0000_0001.ndjson");
        URL urlJobNotExpiredYetConnection = this.getClass().getResource("/" + TEST_FILE);
        Path sourceJobNotExpiredYetConnection = Paths.get(urlJobNotExpiredYetConnection.toURI());
        Files.copy(sourceJobNotExpiredYetConnection, destinationJobNotExpiredYetConnection, StandardCopyOption.REPLACE_EXISTING);

        fileDeletionService.deleteFiles();

        assertTrue(Files.exists(destinationJobNotExpiredYetConnection));
    }
//
//    @DisplayName("Delete all empty directories")
//    @Test
//    void deleteAllExpiredEmptyDirectories() throws IOException, URISyntaxException {
//        Path jobPath = Paths.get(efsMount, job.getJobUuid());
//        File jobDir = new File(jobPath.toString());
//        if (!jobDir.exists()) jobDir.mkdirs();
//
//        changeFileCreationDate(jobPath);
//
//        fileDeletionService.deleteFiles();
//    }

    @Test
    public void testEFSMountSlash() {
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", "~/UsersDir");

        var exceptionThrown = assertThrows(EFSMountFormatException.class, () ->
                fileDeletionService.deleteFiles());
        assertThat(exceptionThrown.getMessage(), is("EFS Mount must start with a /"));
    }

    @Test
    public void testEFSMountDirectorySize() {
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", "/a");

        var exceptionThrown = assertThrows(EFSMountFormatException.class, () ->
                fileDeletionService.deleteFiles());
        assertThat(exceptionThrown.getMessage(), is("EFS mount must be at least 5 characters"));
    }

    @Test
    public void testEFSMountDirectoryBlacklist() {
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", "/usr/baddirectory");

        var exceptionThrown = assertThrows(EFSMountFormatException.class, () ->
                fileDeletionService.deleteFiles());
        assertThat(exceptionThrown.getMessage(), is("EFS mount must not start with a directory that contains important files"));
    }

    @Test
    public void testEFSMountOptAb2d() {
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", "/opt/ab2d");

        // Confirm no exceptions thrown
        fileDeletionService.deleteFiles();
    }

    @Test
    public void testEFSMountOpt() {
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", "/opt");

        var exceptionThrown = assertThrows(EFSMountFormatException.class, () ->
                fileDeletionService.deleteFiles());
        assertThat(exceptionThrown.getMessage(), is("EFS mount must be at least 5 characters"));
    }

    private void checkNoOtherEventsLogged() {
        assertTrue(UtilMethods.allEmpty(
                doAll.load(ApiRequestEvent.class),
                doAll.load(ApiResponseEvent.class),
                doAll.load(ReloadEvent.class),
                doAll.load(ContractBeneSearchEvent.class),
                doAll.load(ErrorEvent.class),
                doAll.load(JobStatusChangeEvent.class)));
    }
}
