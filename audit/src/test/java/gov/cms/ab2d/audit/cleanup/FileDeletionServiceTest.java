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
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import gov.cms.ab2d.fhir.Versions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.audit.properties")
@Testcontainers
class FileDeletionServiceTest {

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
    private LoggerEventRepository loggerEventRepository;

    private static final String TEST_FILE = "testFile.ndjson";

    private static final String TEST_FILE_NOT_DELETED = "testFileNotDeleted.ndjson";

    private static final String TEST_DIRECTORY_NO_PERMISSIONS = "testDirectoryNoPermissions";

    private static final String TEST_FILE_NO_PERMISSIONS = TEST_DIRECTORY_NO_PERMISSIONS + File.separator + "testFileNoPermissions.ndjson";

    private static final String TEST_DIRECTORY = "testDirectory";

    private static final String TEST_FILE_NESTED = TEST_DIRECTORY + File.separator + "testFileInDirectory.ndjson";

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
    private Job jobCancelled;
    private Job jobFailed;
    private String efsMount;

    private List<Path> pathsToDelete;

    @BeforeEach
    public void init() {
        pathsToDelete = new ArrayList<>();

        User user = dataSetup.setupUser(List.of());

        // Connected to a job that is finished and has expired
        job = new Job();
        job.setStatus(JobStatus.SUCCESSFUL);
        job.setJobUuid(UUID.randomUUID().toString());
        job.setCreatedAt(OffsetDateTime.now().minusDays(5));
        job.setCompletedAt(OffsetDateTime.now().minusDays(4));
        job.setExpiresAt(OffsetDateTime.now().minusDays(1));
        job.setUser(user);
        job.setFhirVersion(Versions.FhirVersions.STU3);
        jobService.updateJob(job);

        // Connected to a job, but in progress
        jobInProgress = new Job();
        jobInProgress.setStatus(JobStatus.IN_PROGRESS);
        jobInProgress.setJobUuid(UUID.randomUUID().toString());
        jobInProgress.setCreatedAt(OffsetDateTime.now().minusHours(1));
        jobInProgress.setUser(user);
        jobInProgress.setFhirVersion(Versions.FhirVersions.STU3);
        jobService.updateJob(jobInProgress);

        // Connected to a job that is finished where the file has yet to expire
        jobNotExpiredYet = new Job();
        jobNotExpiredYet.setStatus(JobStatus.SUCCESSFUL);
        jobNotExpiredYet.setJobUuid(UUID.randomUUID().toString());
        jobNotExpiredYet.setCreatedAt(OffsetDateTime.now().minusHours(60));
        jobNotExpiredYet.setCompletedAt(OffsetDateTime.now().minusHours(55));
        jobNotExpiredYet.setExpiresAt(OffsetDateTime.now().plusHours(17));
        jobNotExpiredYet.setUser(user);
        jobNotExpiredYet.setFhirVersion(Versions.FhirVersions.STU3);

        jobCancelled = new Job();
        jobCancelled.setStatus(JobStatus.CANCELLED);
        jobCancelled.setJobUuid(UUID.randomUUID().toString());
        jobCancelled.setCreatedAt(OffsetDateTime.now().minusHours(1));
        jobCancelled.setUser(user);
        jobCancelled.setFhirVersion(Versions.FhirVersions.STU3);
        jobService.updateJob(jobCancelled);

        jobFailed = new Job();
        jobFailed.setStatus(JobStatus.FAILED);
        jobFailed.setJobUuid(UUID.randomUUID().toString());
        jobFailed.setCreatedAt(OffsetDateTime.now().minusHours(1));
        jobFailed.setUser(user);
        jobFailed.setFhirVersion(Versions.FhirVersions.STU3);
        jobService.updateJob(jobFailed);

        efsMount = tmpDirFolder.toPath().toString();
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", efsMount);
    }

    @AfterEach
    void cleanUp() throws IOException {
        loggerEventRepository.delete();

        for (Path toDelete : pathsToDelete) {

            if (Files.isDirectory(toDelete) && Files.exists(toDelete)) {
                FileSystemUtils.deleteRecursively(toDelete);
            } else if (Files.exists(toDelete)){
                Files.delete(toDelete);
            }
        }
    }

    @DisplayName("Delete unrelated top level ndjson file")
    @Test
    void deleteUnrelatedTopLevelNdjson() throws IOException, URISyntaxException {

        // Not connected to a job
        Path destination = Paths.get(efsMount, TEST_FILE);
        pathsToDelete.add(destination);

        URL url = this.getClass().getResource(File.separator + TEST_FILE);
        Path source = Paths.get(url.toURI());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(destination);

        fileDeletionService.deleteFiles();

        assertTrue(Files.notExists(destination));

        List<LoggableEvent> fileEvents = loggerEventRepository.load(FileEvent.class);
        FileEvent e1 = (FileEvent) fileEvents.get(0);
        assertTrue(e1.getFileName().equalsIgnoreCase(destination.toString()));

        checkNoOtherEventsLogged();
    }

    @DisplayName("Ignore unrelated ndjson file that was just created")
    @Test
    void ignoreNewlyCreatedNestedNdjson() throws IOException, URISyntaxException {
        // Don't change the creation date on this file, but do so on the next ones
        Path destinationNotDeleted = Paths.get(efsMount, TEST_FILE_NOT_DELETED);
        pathsToDelete.add(destinationNotDeleted);

        URL urlNotDeletedFile = this.getClass().getResource(File.separator + TEST_FILE_NOT_DELETED);
        Path sourceNotDeleted = Paths.get(urlNotDeletedFile.toURI());
        Files.copy(sourceNotDeleted, destinationNotDeleted, StandardCopyOption.REPLACE_EXISTING);

        fileDeletionService.deleteFiles();

        assertTrue(Files.exists(destinationNotDeleted));

        checkNoOtherEventsLogged();
    }

    @DisplayName("Delete nested ndjson file not attached to a job")
    @Test
    void deleteNestedNdjson() throws IOException, URISyntaxException {

        final Path dirPath = Paths.get(efsMount, TEST_DIRECTORY);
        File dir = new File(dirPath.toString());
        if (!dir.exists()) dir.mkdirs();
        pathsToDelete.add(dirPath);

        // Not connected to a job
        Path nestedFileDestination = Paths.get(efsMount, TEST_FILE_NESTED);
        URL nestedFileUrl = this.getClass().getResource(File.separator + TEST_FILE_NESTED);
        Path nestedFileSource = Paths.get(nestedFileUrl.toURI());
        Files.copy(nestedFileSource, nestedFileDestination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(nestedFileDestination);

        fileDeletionService.deleteFiles();

        assertTrue(Files.notExists(nestedFileDestination));
        assertTrue(Files.exists(dirPath));

        List<LoggableEvent> fileEvents = loggerEventRepository.load(FileEvent.class);
        FileEvent e1 = (FileEvent) fileEvents.get(0);
        assertTrue(e1.getFileName().equalsIgnoreCase(nestedFileDestination.toString()));

        checkNoOtherEventsLogged();
    }

    @DisplayName("Ignore regular files without ndjson extension")
    @Test
    void ignoreRegularFiles() throws IOException, URISyntaxException {

        Path regularFileDestination = Paths.get(efsMount, REGULAR_FILE);
        pathsToDelete.add(regularFileDestination);

        URL regularFileUrl = this.getClass().getResource(File.separator + REGULAR_FILE);
        Path regularFileSource = Paths.get(regularFileUrl.toURI());
        Files.copy(regularFileSource, regularFileDestination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(regularFileDestination);

        fileDeletionService.deleteFiles();

        assertTrue(Files.exists(regularFileDestination));

        checkNoOtherEventsLogged();
    }

    @DisplayName("Ignore empty directories not associated with a job")
    @Test
    void ignoreEmptyDirectories() throws IOException, URISyntaxException {

        Path regularFolder = Paths.get(efsMount, TEST_DIRECTORY);
        pathsToDelete.add(regularFolder);

        URL regularFileUrl = this.getClass().getResource(File.separator + REGULAR_FILE);
        Path regularFileSource = Paths.get(regularFileUrl.toURI());
        Files.copy(regularFileSource, regularFolder, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(regularFolder);

        fileDeletionService.deleteFiles();

        assertTrue(Files.exists(regularFolder));

        checkNoOtherEventsLogged();
    }

    @DisplayName("Ignore directory if no permissions")
    @Test
    void ignoreIfNoPermissions() throws IOException, URISyntaxException {
        // A directory with no permissions that isn't going to be deleted
        final Path noPermissionsDirPath = Paths.get(efsMount, TEST_DIRECTORY_NO_PERMISSIONS);
        pathsToDelete.add(noPermissionsDirPath);

        File noPermissionsDir = new File(noPermissionsDirPath.toString());
        if (!noPermissionsDir.exists()) noPermissionsDir.mkdirs();

        Path noPermissionsFileDestination = Paths.get(efsMount, TEST_FILE_NO_PERMISSIONS);
        URL noPermissionsFileUrl = this.getClass().getResource(File.separator + TEST_FILE_NO_PERMISSIONS);
        Path noPermissionsFileSource = Paths.get(noPermissionsFileUrl.toURI());
        Files.copy(noPermissionsFileSource, noPermissionsFileDestination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(noPermissionsFileDestination);

        noPermissionsDir.setWritable(false);

        fileDeletionService.deleteFiles();

        assertTrue(Files.exists(noPermissionsFileDestination));

        noPermissionsDir.setWritable(true);

        checkNoOtherEventsLogged();
    }

    @DisplayName("Delete empty folder after job files are deleted")
    @Test
    void deleteCompletedAndExpiredJobFolder() throws IOException, URISyntaxException {

        Path jobPath = Paths.get(efsMount, job.getJobUuid());
        pathsToDelete.add(jobPath);

        File jobDir = new File(jobPath.toString());
        if (!jobDir.exists()) jobDir.mkdirs();

        changeFileCreationDate(jobPath);

        fileDeletionService.deleteFiles();

        assertTrue(Files.notExists(jobPath));

        checkNoOtherEventsLogged();
    }

    @DisplayName("Delete job files for successful jobs after they have expired")
    @Test
    void deleteCompletedAndExpiredJobFiles() throws IOException, URISyntaxException {

        Path jobPath = Paths.get(efsMount, job.getJobUuid());
        File jobDir = new File(jobPath.toString());
        if (!jobDir.exists()) jobDir.mkdirs();
        pathsToDelete.add(jobPath);

        Path destinationJobConnection = Paths.get(jobPath.toString(), "S0000_0001.ndjson");
        URL urlJobConnection = this.getClass().getResource(File.separator + TEST_FILE);
        Path sourceJobConnection = Paths.get(urlJobConnection.toURI());
        Files.copy(sourceJobConnection, destinationJobConnection, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(destinationJobConnection);

        fileDeletionService.deleteFiles();

        assertTrue(Files.notExists(jobPath));
        assertTrue(Files.notExists(destinationJobConnection));

        List<LoggableEvent> fileEvents = loggerEventRepository.load(FileEvent.class);
        FileEvent e1 = (FileEvent) fileEvents.get(0);
        assertTrue(e1.getFileName().equalsIgnoreCase(destinationJobConnection.toString()));

        checkNoOtherEventsLogged();
    }

    @DisplayName("Delete job files for cancelled jobs immediately")
    @Test
    void deleteCancelledAndExpiredJobFiles() throws IOException, URISyntaxException {

        Path jobPath = Paths.get(efsMount, jobCancelled.getJobUuid());
        File jobDir = new File(jobPath.toString());
        if (!jobDir.exists()) jobDir.mkdirs();
        pathsToDelete.add(jobPath);

        Path destinationJobConnection = Paths.get(jobPath.toString(), "S0000_0001.ndjson");
        URL urlJobConnection = this.getClass().getResource(File.separator + TEST_FILE);
        Path sourceJobConnection = Paths.get(urlJobConnection.toURI());
        Files.copy(sourceJobConnection, destinationJobConnection, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(destinationJobConnection);

        fileDeletionService.deleteFiles();

        assertTrue(Files.notExists(jobPath));
        assertTrue(Files.notExists(destinationJobConnection));

        List<LoggableEvent> fileEvents = loggerEventRepository.load(FileEvent.class);
        FileEvent e1 = (FileEvent) fileEvents.get(0);
        assertTrue(e1.getFileName().equalsIgnoreCase(destinationJobConnection.toString()));

        checkNoOtherEventsLogged();
    }

    @DisplayName("Delete job files for failed jobs immediately")
    @Test
    void deleteFailedAndExpiredJobFiles() throws IOException, URISyntaxException {

        Path jobPath = Paths.get(efsMount, jobFailed.getJobUuid());
        File jobDir = new File(jobPath.toString());
        if (!jobDir.exists()) jobDir.mkdirs();
        pathsToDelete.add(jobPath);

        Path destinationJobConnection = Paths.get(jobPath.toString(), "S0000_0001.ndjson");
        URL urlJobConnection = this.getClass().getResource(File.separator + TEST_FILE);
        Path sourceJobConnection = Paths.get(urlJobConnection.toURI());
        Files.copy(sourceJobConnection, destinationJobConnection, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(destinationJobConnection);

        fileDeletionService.deleteFiles();

        assertTrue(Files.notExists(jobPath));
        assertTrue(Files.notExists(destinationJobConnection));

        List<LoggableEvent> fileEvents = loggerEventRepository.load(FileEvent.class);
        FileEvent e1 = (FileEvent) fileEvents.get(0);
        assertTrue(e1.getFileName().equalsIgnoreCase(destinationJobConnection.toString()));

        checkNoOtherEventsLogged();
    }

    @DisplayName("Ignore in progress job files")
    @Test
    void ignoreInProgressJobFiles() throws IOException, URISyntaxException {

        Path jobInProgressPath = Paths.get(efsMount, jobInProgress.getJobUuid());
        File jobInProgressDir = new File(jobInProgressPath.toString());
        pathsToDelete.add(jobInProgressPath);

        if (!jobInProgressDir.exists()) jobInProgressDir.mkdirs();
        Path destinationJobInProgressConnection = Paths.get(jobInProgressPath.toString(), "S0000_0001.ndjson");
        URL urlJobInProgressConnection = this.getClass().getResource(File.separator + TEST_FILE);
        Path sourceJobInProgressConnection = Paths.get(urlJobInProgressConnection.toURI());
        Files.copy(sourceJobInProgressConnection, destinationJobInProgressConnection, StandardCopyOption.REPLACE_EXISTING);

        assertTrue(Files.exists(destinationJobInProgressConnection));

        checkNoOtherEventsLogged();
    }

    @DisplayName("Ignore in progress job files")
    @Test
    void ignoreInProgressJobFolders() throws IOException {

        Path jobInProgressPath = Paths.get(efsMount, jobInProgress.getJobUuid());
        File jobInProgressDir = new File(jobInProgressPath.toString());
        if (!jobInProgressDir.exists()) jobInProgressDir.mkdirs();
        pathsToDelete.add(jobInProgressPath);

        fileDeletionService.deleteFiles();

        assertTrue(Files.exists(jobInProgressPath));

        checkNoOtherEventsLogged();
    }

    private void checkNoOtherEventsLogged() {
        assertTrue(UtilMethods.allEmpty(
                loggerEventRepository.load(ApiRequestEvent.class),
                loggerEventRepository.load(ApiResponseEvent.class),
                loggerEventRepository.load(ReloadEvent.class),
                loggerEventRepository.load(ContractBeneSearchEvent.class),
                loggerEventRepository.load(ErrorEvent.class),
                loggerEventRepository.load(JobStatusChangeEvent.class)));
    }

    @DisplayName("Ignore recently completed job files")
    @Test
    void ignoreRecentlyCompletedJobFiles() throws IOException, URISyntaxException {

        jobService.updateJob(jobNotExpiredYet);
        Path jobNotExpiredYetPath = Paths.get(efsMount, jobNotExpiredYet.getJobUuid());
        File jobNotExpiredYetDir = new File(jobNotExpiredYetPath.toString());
        if (!jobNotExpiredYetDir.exists()) jobNotExpiredYetDir.mkdirs();
        pathsToDelete.add(jobNotExpiredYetPath);

        Path destinationJobNotExpiredYetConnection = Paths.get(jobNotExpiredYetPath.toString(), "S0000_0001.ndjson");
        URL urlJobNotExpiredYetConnection = this.getClass().getResource(File.separator + TEST_FILE);
        Path sourceJobNotExpiredYetConnection = Paths.get(urlJobNotExpiredYetConnection.toURI());
        Files.copy(sourceJobNotExpiredYetConnection, destinationJobNotExpiredYetConnection, StandardCopyOption.REPLACE_EXISTING);

        fileDeletionService.deleteFiles();

        assertTrue(Files.exists(destinationJobNotExpiredYetConnection));
    }

    @DisplayName("Ignore recently completed job folders")
    @Test
    void ignoreRecentlyCompletedJobFolders() throws IOException, URISyntaxException {

        jobService.updateJob(jobNotExpiredYet);
        Path jobNotExpiredYetPath = Paths.get(efsMount, jobNotExpiredYet.getJobUuid());
        File jobNotExpiredYetDir = new File(jobNotExpiredYetPath.toString());
        if (!jobNotExpiredYetDir.exists()) jobNotExpiredYetDir.mkdirs();
        pathsToDelete.add(jobNotExpiredYetPath);

        fileDeletionService.deleteFiles();

        assertTrue(Files.exists(jobNotExpiredYetPath));
    }

    // Folder checking only works on unix systems
    @EnabledOnOs(value = {OS.LINUX, OS.MAC})
    @ParameterizedTest
    @MethodSource("directoryAndMessage")
    void testEFSMountChecks(String dirToSet, String exceptionMessage) {
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", dirToSet);

        var exceptionThrown = assertThrows(EFSMountFormatException.class, () ->
                fileDeletionService.deleteFiles());
        assertThat(exceptionThrown.getMessage(), is(exceptionMessage));
    }

    static Stream<Arguments> directoryAndMessage() {
        return Stream.of(
                arguments("~/UsersDir", "EFS Mount must start with a /"),
                arguments("/a", "EFS mount must be at least 5 characters"),
                arguments("/usr/baddirectory",
                        "EFS mount must not start with a directory that contains important files"),
                arguments("/opt","EFS mount must be at least 5 characters")
        );
    }

    // Only a useful check on unix systems
    @EnabledOnOs(value = {OS.LINUX, OS.MAC})
    @Test
    void testEFSMountOptAb2d() {
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", "/opt/ab2d");

        // Confirm no exceptions thrown
        fileDeletionService.deleteFiles();
    }
}
