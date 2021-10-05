package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.SinceSource;
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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static gov.cms.ab2d.fhir.FhirVersion.R4;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
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
    void checkJobExistsBeforeProcessing() {

        var exceptionThrown = assertThrows(
                IllegalArgumentException.class,
                () -> cut.preprocess("missing-job-id"));

        assertEquals("Job missing-job-id was not found", exceptionThrown.getMessage());
    }

    @Test
    @DisplayName("Throws exception when the job for the given JobUuid is not in submitted status")
    void whenTheJobForTheGivenJobUuidIsNotInSubmittedStatus_ThrowsException() {

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

    @DisplayName("Test to see if default since behavior does not run for STU3")
    @Test
    void testDefaultSinceSTU3() {
        // Test if it's STU3, nothing changes since default 'since' is not defined for STU3
        Job job = createJob();
        job.setStatus(JobStatus.SUBMITTED);
        when(jobRepository.findByJobUuid(job.getJobUuid())).thenReturn(job);
        cut.preprocess(job.getJobUuid());
        assertNull(job.getSince());
        verify(jobRepository, never()).findFirstByContractEqualsAndStatusInAndStartedByOrderByCompletedAtDesc(any(), any(), any());
        assertNull(job.getSinceSource());
    }

    @DisplayName("Test to see if default since behavior runs for R4, but doesn't work if there was no previous successful job")
    @Test
    void testDefaultSinceR4FirstRun() {
        Job job = createJob();
        job.setFhirVersion(R4);
        job.setStatus(JobStatus.SUBMITTED);
        when(jobRepository.findByJobUuid(job.getJobUuid())).thenReturn(job);
        when(jobRepository.findFirstByContractEqualsAndStatusInAndStartedByOrderByCompletedAtDesc(any(), any(), any())).thenReturn(Optional.empty());
        cut.preprocess(job.getJobUuid());
        assertNull(job.getSince());
        assertEquals(SinceSource.FIRST_RUN, job.getSinceSource());
    }

    @DisplayName("has had a successful run, set since as null, populate it")
    @Test
    void testDefaultSinceR4FirstRunByPdp() {
        Job newJob = createJob();
        newJob.setFhirVersion(R4);
        newJob.setStatus(JobStatus.SUBMITTED);
        newJob.setCreatedAt(OffsetDateTime.now());

        // Old job can still be STU3 and still count
        Job oldJob = createJob();
        oldJob.setStatus(JobStatus.SUCCESSFUL);
        oldJob.setJobUuid(oldJob.getJobUuid() + "-2");
        OffsetDateTime oldJobTime = OffsetDateTime.parse("2021-01-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        oldJob.setCreatedAt(oldJobTime);

        when(jobRepository.findByJobUuid(newJob.getJobUuid())).thenReturn(newJob);
        when(jobRepository.findFirstByContractEqualsAndStatusInAndStartedByOrderByCompletedAtDesc(any(), any(), any())).thenReturn(Optional.of(oldJob));

        cut.preprocess(newJob.getJobUuid());
        assertEquals(oldJobTime, newJob.getSince());
        assertEquals(SinceSource.AB2D, newJob.getSinceSource());
    }

    @DisplayName("has had a successful run, since is set so don't populate it")
    @Test
    void testDefaultSinceR4SuppliedSince() {
        Job newJob = createJob();
        newJob.setFhirVersion(R4);
        newJob.setStatus(JobStatus.SUBMITTED);
        newJob.setCreatedAt(OffsetDateTime.now());
        OffsetDateTime now = OffsetDateTime.now();
        newJob.setSince(now);

        // Old job can still be STU3 and still count
        Job oldJob = createJob();
        oldJob.setStatus(JobStatus.SUCCESSFUL);
        OffsetDateTime oldJobTime = OffsetDateTime.parse("2021-01-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        oldJob.setCreatedAt(oldJobTime);
        oldJob.setJobUuid(oldJob.getJobUuid() + "-2");

        when(jobRepository.findByJobUuid(newJob.getJobUuid())).thenReturn(newJob);
        when(jobRepository.findByJobUuid(oldJob.getJobUuid())).thenReturn(oldJob);

        cut.preprocess(newJob.getJobUuid());

        verify(jobRepository, never()).findFirstByContractEqualsAndStatusInAndStartedByOrderByCompletedAtDesc(any(), any(), any());
        assertEquals(now, newJob.getSince());
        assertEquals(SinceSource.USER, newJob.getSinceSource());
    }

    // Not first run, since supplied

    private Job createJob() {
        Job job = new Job();
        job.setJobUuid(JOB_UUID);
        job.setStatusMessage("0%");
        job.setFhirVersion(STU3);

        return job;
    }
}