package gov.cms.ab2d.audit.cleanup;

import com.amazonaws.services.directconnect.model.MacSecKey;
import gov.cms.ab2d.audit.SpringBootApp;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.service.JobOutputServiceImpl;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

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

    @MockBean
    private JobOutputRepository jobOutputRepository;

    @Autowired
    @InjectMocks
    private JobOutputServiceImpl jobOutputService;

    @Autowired
    private FileDeletionServiceImpl fileDeletionService;

    @BeforeEach
    public void init() {
        efsMount = tmpDirFolder.toPath().toString();
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", efsMount);

    }

    @Test
    void downloadedFileDelete() throws IOException {
        List<JobOutput> testData = parametrizedTest(1, 1);
        for (JobOutput jobOutput : testData) {
            assertFalse(Path.of(efsMount, jobOutput.getJob().getJobUuid(), jobOutput.getFilePath()).toFile().exists());
        }
    }

    @Test
    void downloadedFileDeleteMultipleJobs() throws IOException {
        List<JobOutput> testData = parametrizedTest(5, 1);
        for (JobOutput jobOutput : testData) {
            assertFalse(Path.of(efsMount, jobOutput.getJob().getJobUuid(), jobOutput.getFilePath()).toFile().exists());
        }
    }

    @Test
    void downloadedFileDeleteMultipleFiles() throws IOException {
        List<JobOutput> testData = parametrizedTest(1, 5);
        for (JobOutput jobOutput : testData) {
            assertFalse(Path.of(efsMount, jobOutput.getJob().getJobUuid(), jobOutput.getFilePath()).toFile().exists());
        }
    }

    @Test
    void t() throws IOException {
        List<JobOutput> testData = createTestData(1, 1);
        try (MockedStatic<Files> filesMockedStatic = Mockito.mockStatic(Files.class)) {
            filesMockedStatic.when(() -> Files.delete(any(Path.class))).thenThrow(new IOException());
            filesMockedStatic.when(() -> Files.isRegularFile(any(Path.class))).thenReturn(true);
            Mockito.when(jobOutputRepository.findByDownloadExpiredAndJobExpired(30))
                    .thenReturn(Optional.of(testData));
            assertDoesNotThrow(() -> fileDeletionService.deleteDownloadIntervalExpiredFiles());
        }
    }

    private List<JobOutput> parametrizedTest(int jobs, int files) throws IOException {
        List<JobOutput> testData = createTestData(jobs, files);
        Mockito.when(jobOutputRepository.findByDownloadExpiredAndJobExpired(30))
                .thenReturn(Optional.of(testData));
        fileDeletionService.deleteDownloadIntervalExpiredFiles();
        return testData;
    }

    public List<JobOutput> createTestData(int jobs, int files) throws IOException {
        List<JobOutput> jobOutputList = new ArrayList<>();
        for (int jobCount = 0; jobCount < jobs; jobCount++) {
            Job job = new Job();
            String uuid = RandomStringUtils.randomAlphanumeric(5);
            Path uuidPath = Path.of(efsMount, uuid);
            if (!uuidPath.toFile().exists()) {
                Files.createDirectory(uuidPath);
            }
            job.setJobUuid(uuid);
            job.setOrganization(RandomStringUtils.randomAlphanumeric(6));
            for (int fileCount = 0; fileCount < files; fileCount++) {
                JobOutput jobOutput = new JobOutput();
                jobOutput.setJob(job);
                String fileName = RandomStringUtils.randomAlphanumeric(10) + ".ndjson";
                jobOutput.setFilePath(fileName);
                Files.createFile(Path.of(efsMount, uuid, fileName));
                jobOutputList.add(jobOutput);
            }
        }
        return jobOutputList;
    }


}
