package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class JobPreProcessorUnitTest {
    // class under test
    private JobPreProcessor cut;

    private static final String JOB_UUID = "6d08bf08-f926-4e19-8d89-ad67ef89f17e";

    @Mock
    private JobRepository jobRepository;
    @Mock
    private LogManager eventLogger;
    @Mock
    private CoverageDriver coverageDriver;

    private Job job;

    @BeforeEach
    void setUp() {
        cut = new JobPreProcessorImpl(jobRepository, eventLogger, coverageDriver);
        job = createJob();
    }

    @DisplayName("Do not start a job which is not saved in the database")
    @Test
    void checkJobExistsBeforeProcessing() throws InterruptedException {

        var exceptionThrown = assertThrows(
                IllegalArgumentException.class,
                () -> cut.preprocess("missing-job-id"));

        assertEquals("Job missing-job-id was not found", exceptionThrown.getMessage());
    }

    @Test
    @DisplayName("Throws exception when the job for the given JobUuid is not in submitted status")
    void whenTheJobForTheGivenJobUuidIsNotInSubmittedStatus_ThrowsException() throws InterruptedException {

        when(jobRepository.findByJobUuid(job.getJobUuid())).thenReturn(job);

        var exceptionThrown = assertThrows(
                IllegalArgumentException.class,
                () -> cut.preprocess(job.getJobUuid()));

        assertEquals(String.format("Job %s is not in SUBMITTED status", JOB_UUID), exceptionThrown.getMessage());
    }

    @Test
    @DisplayName("When a job is in submitted status, it will be moved to IN_PROGRESS status during pre-processing")
    void whenJobIsInSubmittedStatus_ThenJobShouldBePutInProgress() throws InterruptedException {
        job.setStatus(JobStatus.SUBMITTED);

        when(jobRepository.save(Mockito.any())).thenReturn(job);
        when(jobRepository.findByJobUuid(job.getJobUuid())).thenReturn(job);
        when(coverageDriver.isCoverageAvailable(any(Job.class))).thenReturn(true);

        var processedJob = cut.preprocess(job.getJobUuid());

        assertEquals(JobStatus.IN_PROGRESS, processedJob.getStatus());
        verify(jobRepository).save(Mockito.any());
    }

    @DisplayName("Job is not started if coverage is not available")
    @Test
    void proccessingNotTriggeredIfCoverageNotAvailable() throws InterruptedException {

        job.setStatus(JobStatus.SUBMITTED);
        when(jobRepository.findByJobUuid(job.getJobUuid())).thenReturn(job);
        when(coverageDriver.isCoverageAvailable(any(Job.class))).thenReturn(false);

        Job result = cut.preprocess(job.getJobUuid());
        assertEquals(JobStatus.SUBMITTED, result.getStatus());
    }

    @DisplayName("Job is not started if coverage check is interrupted")
    @Test
    void proccessingNotTriggeredIfCoverageCheckInterrupted() throws InterruptedException {

        job.setStatus(JobStatus.SUBMITTED);
        when(jobRepository.findByJobUuid(job.getJobUuid())).thenReturn(job);
        when(coverageDriver.isCoverageAvailable(any(Job.class))).thenThrow(InterruptedException.class);

        var exceptionThrown = assertThrows(RuntimeException.class,
                () -> cut.preprocess(job.getJobUuid()));
        assertEquals("could not determine whether coverage metadata was up to date",
                exceptionThrown.getMessage());
    }

    private Job createJob() {
        Job job = new Job();
        job.setJobUuid(JOB_UUID);
        job.setStatusMessage("0%");
        return job;
    }
}