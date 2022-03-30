package gov.cms.ab2d.audit.cleanup;

import gov.cms.ab2d.audit.SpringBootApp;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.service.JobOutputServiceImpl;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
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

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.audit.properties")
@Testcontainers
@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class FileDeleteDownloadExpiredTest {

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @MockBean
    private JobOutputRepository jobOutputRepository;

    @Autowired
    @InjectMocks
    private JobOutputServiceImpl jobOutputService;

    @Autowired
    private FileDeletionServiceImpl fileDeletionService;


    @Test
    void downloadedFileDelete(@TempDir File tmpDir) throws IOException {
        List<JobOutput> testData = parametrizedTest(1, 1, tmpDir);
        for (JobOutput jobOutput : testData) {
            assertFalse(Path.of(tmpDir.getAbsolutePath(), jobOutput.getJob().getJobUuid(), jobOutput.getFilePath()).toFile().exists());
        }
    }

    @Test
    void downloadedFileDeleteMultipleJobs(@TempDir File tmpDir) throws IOException {
        List<JobOutput> testData = parametrizedTest(5, 1, tmpDir);
        for (JobOutput jobOutput : testData) {
            assertFalse(Path.of(tmpDir.getAbsolutePath(), jobOutput.getJob().getJobUuid(), jobOutput.getFilePath()).toFile().exists());
        }
    }

    @Test
    void downloadedFileDeleteMultipleFiles(@TempDir File tmpDir) throws IOException {
        List<JobOutput> testData = parametrizedTest(1, 5, tmpDir);
        for (JobOutput jobOutput : testData) {
            assertFalse(Path.of(tmpDir.getAbsolutePath(), jobOutput.getJob().getJobUuid(), jobOutput.getFilePath()).toFile().exists());
        }
    }

    private List<JobOutput> parametrizedTest(int jobs, int files, File tmpDir) throws IOException {
        List<JobOutput> testData = createTestData(jobs, files, tmpDir);
        Mockito.when(jobOutputRepository.findByDownloadExpiredAndJobExpired(30))
                .thenReturn(Optional.of(testData));
        fileDeletionService.deleteDownloadIntervalExpiredFiles();
        return testData;
    }


    private List<JobOutput> createTestData(int jobs, int files, File tmpDir) throws IOException {
        List<JobOutput> jobOutputList = new ArrayList<>();
        for (int jobCount = 0; jobCount < jobs; jobCount++) {
            Job job = new Job();
            String uuid = RandomStringUtils.random(5);
            Path uuidPath = Path.of(tmpDir.getAbsolutePath(), uuid);
            if (!uuidPath.toFile().exists()) {
                Files.createFile(uuidPath);
            }
            job.setJobUuid(uuid);
            job.setOrganization(RandomStringUtils.random(6));
            for (int fileCount = 0; fileCount < files; fileCount++) {
                JobOutput jobOutput = new JobOutput();
                jobOutput.setJob(job);
                String fileName = RandomStringUtils.random(10) + ".ndjson";
                jobOutput.setFilePath(fileName);
                Files.createFile(Path.of(tmpDir.getAbsolutePath(), fileName));
                jobOutputList.add(jobOutput);
            }
        }
        return jobOutputList;
    }

}
