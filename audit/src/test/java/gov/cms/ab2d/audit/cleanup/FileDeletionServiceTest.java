package gov.cms.ab2d.audit.cleanup;

import gov.cms.ab2d.audit.SpringBootApp;
import gov.cms.ab2d.audit.dto.AuditMockJob;
import gov.cms.ab2d.audit.remote.JobAuditClientMock;
import gov.cms.ab2d.common.dto.StaleJob;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.eventlogger.events.ContractSearchEvent;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.eventlogger.events.JobStatusChangeEvent;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gov.cms.ab2d.common.model.JobStatus.CANCELLED;
import static gov.cms.ab2d.common.model.JobStatus.FAILED;
import static gov.cms.ab2d.common.model.JobStatus.IN_PROGRESS;
import static gov.cms.ab2d.common.model.JobStatus.SUCCESSFUL;
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
    private FileDeletionServiceImpl fileDeletionService;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private JobAuditClientMock jobAuditClientMock;

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

    private AuditMockJob job;
    private AuditMockJob jobInProgress;
    private AuditMockJob jobNotExpiredYet;
    private AuditMockJob jobCancelled;
    private AuditMockJob jobFailed;
    private String efsMount;

    private List<Path> pathsToDelete;

    @BeforeEach
    public void init() {
        final String contractNumber = "FileJob123";
        pathsToDelete = new ArrayList<>();

        PdpClient pdpClient = dataSetup.setupPdpClient(List.of());

        // Connected to a job that is finished and has expired
        job = new AuditMockJob(new StaleJob(UUID.randomUUID().toString(), pdpClient.getOrganization()),
                SUCCESSFUL, OffsetDateTime.now().minusDays(4));
        jobAuditClientMock.update(job);

        // Connected to a job, but in progress
        jobInProgress = new AuditMockJob(new StaleJob(UUID.randomUUID().toString(), pdpClient.getOrganization()),
                IN_PROGRESS, null);
        jobAuditClientMock.update(job);

        // Connected to a job that is finished where the file has yet to expire
        jobNotExpiredYet = new AuditMockJob(new StaleJob(UUID.randomUUID().toString(), pdpClient.getOrganization()),
                SUCCESSFUL, OffsetDateTime.now().minusHours(55));
        jobAuditClientMock.update(job);

        jobCancelled = new AuditMockJob(new StaleJob(UUID.randomUUID().toString(), pdpClient.getOrganization()),
                CANCELLED, null);
        jobAuditClientMock.update(job);

        jobFailed = new AuditMockJob(new StaleJob(UUID.randomUUID().toString(), pdpClient.getOrganization()),
                FAILED, null);
        jobAuditClientMock.update(job);

        efsMount = tmpDirFolder.toPath().toString();
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", efsMount);
    }

    @AfterEach
    void cleanUp() throws IOException {
        loggerEventRepository.delete();

        for (Path toDelete : pathsToDelete) {

            if (Files.isDirectory(toDelete) && Files.exists(toDelete)) {
                FileSystemUtils.deleteRecursively(toDelete);
            } else if (Files.exists(toDelete)) {
                Files.delete(toDelete);
            }
        }

        jobAuditClientMock.cleanup();
    }

    @DisplayName("Do not delete unrelated top level ndjson file")
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

        assertFalse(Files.notExists(destination));

        List<LoggableEvent> fileEvents = loggerEventRepository.load(FileEvent.class);
        assertEquals(0, fileEvents.size());

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

        assertTrue(Files.exists(nestedFileDestination));
        assertTrue(Files.exists(dirPath));

        List<LoggableEvent> fileEvents = loggerEventRepository.load(FileEvent.class);
        assertEquals(0, fileEvents.size());

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
                loggerEventRepository.load(ContractSearchEvent.class),
                loggerEventRepository.load(ErrorEvent.class),
                loggerEventRepository.load(JobStatusChangeEvent.class)));
    }

    @DisplayName("Ignore recently completed job files")
    @Test
    void ignoreRecentlyCompletedJobFiles() throws IOException, URISyntaxException {

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
                arguments("/opt", "EFS mount must be at least 5 characters")
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

    @Test
    void testAggregatorStuff(@TempDir File tmpDir) throws IOException {
        String miscString = "Hello";
        String jobId = job.getJobUuid();

        ReflectionTestUtils.setField(fileDeletionService, "efsMount", tmpDir.getAbsolutePath());

        // Create the directories
        Path jobDir = Files.createDirectory(Path.of(tmpDir.getAbsolutePath(), jobId));
        Path finishedDir = Files.createDirectory(Path.of(tmpDir.getAbsolutePath(), jobId, "finished"));
        Path streamDir = Files.createDirectory(Path.of(tmpDir.getAbsolutePath(), jobId, "streaming"));

        // Create the files
        List<Path> files = new ArrayList<>();
        files.add(Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "tstfile.ndjson")));
        files.add(Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "tstfile.txt")));
        files.add(Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "finished", "tstfile.ndjson")));
        files.add(Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "streaming", "tstfile.ndjson")));
        files.add(Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "streaming", "tstfile.txt")));

        // Write data to the files
        for (Path file : files) {
            Files.writeString(file, miscString);
            changeFileCreationDate(file);
        }

        // Update the creation time of the directories after you add the files because putting files in the dir changes
        // its time.
        changeFileCreationDate(jobDir);
        changeFileCreationDate(finishedDir);
        changeFileCreationDate(streamDir);

        fileDeletionService.deleteFiles();

        int numExists = 0;
        for (Path p : files) {
            if (p.toFile().exists()) {
                numExists++;
            }
        }
        assertEquals(2, numExists);

        assertTrue(streamDir.toFile().exists());
        assertFalse(finishedDir.toFile().exists());
    }

    @Test
    void testCleanOutDir(@TempDir File tmpDir) throws IOException {
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", tmpDir.getAbsolutePath());
        String jobId = job.getJobUuid();
        String miscString = "Hello";

        // Create the directories
        List<Path> files = new ArrayList<>();

        Path jobDir = Files.createDirectory(Path.of(tmpDir.getAbsolutePath(), jobId));
        Path finishedDir = Files.createDirectory(Path.of(tmpDir.getAbsolutePath(), jobId, "finished"));
        Path streamDir = Files.createDirectory(Path.of(tmpDir.getAbsolutePath(), jobId, "streaming"));

        // Create the files
        files.add(Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "tstfile.ndjson")));
        files.add(Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "tstfile.txt")));
        files.add(Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "finished", "tstfile.ndjson")));
        files.add(Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "streaming", "tstfile.ndjson")));
        files.add(Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "streaming", "tstfile.txt")));

        // Write data to the files
        for (Path file : files) {
            Files.writeString(file, miscString);
            changeFileCreationDate(file);
        }

        files.add(jobDir);
        files.add(finishedDir);
        files.add(streamDir);

        StaleJob staleJob = new StaleJob(job.getJobUuid(), job.getOrganization());
        fileDeletionService.deleteNdjsonFilesAndDirectory(staleJob, jobDir);

        List<Path> remaining = files.stream().filter(f -> f.toFile().exists()).collect(Collectors.toList());
        // There should be 4 remaining path - the top job directory, the streaming dir, the top level txt file and the s
        // streaming txt file
        assertEquals(4, remaining.size());
        assertEquals(2, remaining.stream().filter(f -> f.toFile().isDirectory()).count());
        assertEquals(2, remaining.stream().filter(f -> f.toFile().isFile())
                .filter(f -> f.toFile().getName().equals("tstfile.txt")).count());
        remaining.forEach(System.out::println);
    }

    @Test
    void testDontCleanOutDir(@TempDir File tmpDir) throws IOException {
        AuditMockJob newJob = new AuditMockJob(new StaleJob(UUID.randomUUID().toString(), job.getOrganization()),
                SUCCESSFUL, OffsetDateTime.now().minusDays(1));
        jobAuditClientMock.update(job);
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", tmpDir.getAbsolutePath());
        String jobId = newJob.getJobUuid();
        String miscString = "Hello";

        // Create the directories
        List<Path> files = new ArrayList<>();

        Path jobDir = Files.createDirectory(Path.of(tmpDir.getAbsolutePath(), jobId));
        Path finishedDir = Files.createDirectory(Path.of(tmpDir.getAbsolutePath(), jobId, "finished"));
        Path streamDir = Files.createDirectory(Path.of(tmpDir.getAbsolutePath(), jobId, "streaming"));

        // Create the files
        files.add(Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "tstfile.ndjson")));
        files.add(Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "tstfile.txt")));
        files.add(Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "finished", "tstfile.ndjson")));
        files.add(Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "streaming", "tstfile.ndjson")));
        files.add(Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "streaming", "tstfile.txt")));

        // Write data to the files
        for (Path file : files) {
            Files.writeString(file, miscString);
        }

        files.add(jobDir);
        files.add(finishedDir);
        files.add(streamDir);

        fileDeletionService.deleteFiles();

        List<Path> remaining = files.stream().filter(f -> f.toFile().exists()).collect(Collectors.toList());
        remaining.forEach(System.out::println);

        assertEquals(8, remaining.size());
        assertEquals(3, remaining.stream().filter(f -> f.toFile().isDirectory()).count());
        assertEquals(2, remaining.stream().filter(f -> f.toFile().isFile())
                .filter(f -> f.toFile().getName().equals("tstfile.txt")).count());
    }

    @Test
    void ignoreNonJobDirs(@TempDir File tmpDir) throws IOException {
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", tmpDir.getAbsolutePath());
        String jobId = job.getJobUuid();

        Files.createFile(Path.of(tmpDir.getAbsolutePath(), "bogusFile.ndjson"));

        // Create the files
        Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "tstfile.ndjson"));
        Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "finished", "tstfile.ndjson"));
        Files.createFile(Path.of(tmpDir.getAbsolutePath(), jobId, "streaming", "tstfile.ndjson"));

        fileDeletionService.deleteFiles();

        File[] files = new File(tmpDir.getAbsolutePath()).listFiles();

        assert files != null;
        Stream.of(files).forEach(System.out::println);

        assertEquals(2, files.length);

        assertEquals(1, Stream.of(files).filter(File::isDirectory).count());
        assertEquals(1, Stream.of(files).filter(File::isFile).count());
    }
}
