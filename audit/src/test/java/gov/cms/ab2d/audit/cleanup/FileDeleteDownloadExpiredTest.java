package gov.cms.ab2d.audit.cleanup;

import gov.cms.ab2d.audit.SpringBootApp;
import gov.cms.ab2d.audit.remote.JobOutputAuditClientMock;
import gov.cms.ab2d.common.dto.StaleJob;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.audit.properties")
@Testcontainers
@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class FileDeleteDownloadExpiredTest {

    private String efsMount;

    @TempDir
    File tmpDirFolder;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();


    @Autowired
    private JobOutputAuditClientMock jobOutputAuditClientMock;

    @Autowired
    private LoggerEventRepository loggerEventRepository;

    @Autowired
    private FileDeletionServiceImpl fileDeletionService;

    @BeforeEach
    public void init() {
        efsMount = tmpDirFolder.toPath().toString();
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", efsMount);

    }

    @Test
    void downloadedFileDelete() throws IOException {
        Map<StaleJob, List<String>> jobFiles = parametrizedTest(1, 1);
        assertDeleted(jobFiles);
    }

    @Test
    void downloadedFileDeleteMultipleJobs() throws IOException {
        Map<StaleJob, List<String>> jobFiles = parametrizedTest(5, 1);
        assertDeleted(jobFiles);
    }

    @Test
    void downloadedFileDeleteMultipleFiles() throws IOException {
        Map<StaleJob, List<String>> jobFiles = parametrizedTest(1, 5);
        assertDeleted(jobFiles);
    }

    @Test
    void fileDeleteFails() throws IOException {
        createTestData(1, 4);
        try (MockedStatic<Files> filesMockedStatic = Mockito.mockStatic(Files.class)) {
            filesMockedStatic.when(() -> Files.delete(any(Path.class))).thenThrow(new IOException());
            filesMockedStatic.when(() -> Files.isRegularFile(any(Path.class))).thenReturn(true);
            assertDoesNotThrow(() -> fileDeletionService.deleteDownloadIntervalExpiredFiles());
        }
    }

    private Map<StaleJob, List<String>> parametrizedTest(int jobs, int files) throws IOException {
        Map<StaleJob, List<String>> testData = createTestData(1, 4);
        fileDeletionService.deleteDownloadIntervalExpiredFiles();
        return testData;
    }

    public Map<StaleJob, List<String>> createTestData(int jobs, int filesCount) throws IOException {
        jobOutputAuditClientMock.cleanup();
        Map<StaleJob, List<String>> jobFiles = new HashMap<>();
        for (int jobCount = 0; jobCount < jobs; jobCount++) {
            String uuid = RandomStringUtils.randomAlphanumeric(5);
            Path uuidPath = Path.of(efsMount, uuid);
            if (!uuidPath.toFile().exists()) {
                Files.createDirectory(uuidPath);
            }
            List<String> files = new ArrayList<>();
            jobFiles.put(new StaleJob(uuid, RandomStringUtils.randomAlphanumeric(6)), files);
            for (int fileCount = 0; fileCount < filesCount; fileCount++) {
                String fileName = RandomStringUtils.randomAlphanumeric(10) + ".ndjson";
                Files.createFile(Path.of(efsMount, uuid, fileName));
                files.add(fileName);
            }
        }
        jobOutputAuditClientMock.update(jobFiles);
        return jobFiles;
    }

    @AfterEach
    void cleanUp() throws IOException {
        loggerEventRepository.delete();
    }

    private void assertDeleted(Map<StaleJob, List<String>> jobFiles) {
        for (Map.Entry<StaleJob, List<String>> jobOutput : jobFiles.entrySet()) {
            for (String file : jobOutput.getValue()) {
                assertFalse(Path.of(efsMount, jobOutput.getKey().getJobUuid(), file).toFile().exists());
            }
        }
    }


}
