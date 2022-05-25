package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.SinceSource;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobOutput;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.util.ContractWorkerClientMock;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static gov.cms.ab2d.common.model.SinceSource.AB2D;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static gov.cms.ab2d.fhir.FhirVersion.R4;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.job.model.JobStatus.SUBMITTED;
import static gov.cms.ab2d.job.model.JobStatus.SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class JobPreProcessorUnitTest {
    // class under test
    private JobPreProcessor cut;

    private static final String JOB_UUID = "6d08bf08-f926-4e19-8d89-ad67ef89f17e";

    private final ContractWorkerClientMock contractWorkerClient = new ContractWorkerClientMock();

    @Mock
    private JobRepository jobRepository;
    @Mock
    private LogManager eventLogger;
    @Mock
    private CoverageDriver coverageDriver;

    private Job job;
    private ContractDTO contract;

    @BeforeEach
    void setUp() {
        contract = new ContractDTO("JPP5678", "JPP5678", null, null);
        cut = new JobPreProcessorImpl(contractWorkerClient, jobRepository, eventLogger, coverageDriver);
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
    @DisplayName("Test to see if the status string is properly formatted for the slack alert")
    void checkStatusString() {
        JobPreProcessorImpl impl = (JobPreProcessorImpl) cut;
        String val = impl.getStatusString(job);
        assertEquals("EOB_JOB_STARTED for JPP5678 in progress", val);
        OffsetDateTime tm = OffsetDateTime.of(2022, 7, 11, 1, 2, 3, 0, ZoneOffset.UTC);
        job.setSince(tm);
        val = impl.getStatusString(job);
        assertEquals("EOB_JOB_STARTED for JPP5678 in progress (since date: 2022-07-11T01:02:03Z)", val);
        ZoneId zoneId = ZoneId.of("Pacific/Honolulu");
        tm = OffsetDateTime.of(2022, 7, 11, 1, 2, 3, 0, ZonedDateTime.now(zoneId).getOffset());
        job.setSince(tm);
        val = impl.getStatusString(job);
        assertEquals("EOB_JOB_STARTED for JPP5678 in progress (since date: 2022-07-11T01:02:03-10:00)", val);
        assertEquals("", impl.getStatusString(null));
        job.setContractNumber(null);
        assertEquals("EOB_JOB_STARTED for (unknown) in progress (since date: 2022-07-11T01:02:03-10:00)", impl.getStatusString(job));
    }

    @Test
    @DisplayName("When a job is in submitted status, it will be moved to IN_PROGRESS status during pre-processing")
    void whenJobIsInSubmittedStatus_ThenJobShouldBePutInProgress() throws InterruptedException {
        job.setStatus(JobStatus.SUBMITTED);

        when(jobRepository.save(Mockito.any())).thenReturn(job);
        when(jobRepository.findByJobUuid(job.getJobUuid())).thenReturn(job);
        when(coverageDriver.isCoverageAvailable(any(Job.class), any(ContractDTO.class))).thenReturn(true);

        var processedJob = cut.preprocess(job.getJobUuid());

        assertEquals(JobStatus.IN_PROGRESS, processedJob.getStatus());
        verify(jobRepository).save(Mockito.any());
    }

    @DisplayName("Job is not started if coverage is not available")
    @Test
    void proccessingNotTriggeredIfCoverageNotAvailable() throws InterruptedException {

        job.setStatus(JobStatus.SUBMITTED);
        when(jobRepository.findByJobUuid(job.getJobUuid())).thenReturn(job);
        when(coverageDriver.isCoverageAvailable(any(Job.class), any(ContractDTO.class))).thenReturn(false);

        Job result = cut.preprocess(job.getJobUuid());
        assertEquals(JobStatus.SUBMITTED, result.getStatus());
    }

    @DisplayName("Job is not started if coverage check is interrupted")
    @Test
    void proccessingNotTriggeredIfCoverageCheckInterrupted() throws InterruptedException {

        job.setStatus(JobStatus.SUBMITTED);
        when(jobRepository.findByJobUuid(job.getJobUuid())).thenReturn(job);
        when(coverageDriver.isCoverageAvailable(any(Job.class), any(ContractDTO.class))).thenThrow(InterruptedException.class);

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
        job.setContractNumber(contract.getContractNumber());
        when(jobRepository.findByJobUuid(job.getJobUuid())).thenReturn(job);
        cut.preprocess(job.getJobUuid());
        assertNull(job.getSince());
        verify(jobRepository, never()).findByContractNumberEqualsAndStatusInAndStartedByOrderByCompletedAtDesc(anyString(), any(), any());
        assertNull(job.getSinceSource());
    }

    @DisplayName("Test to see if default since behavior runs for R4, but doesn't work if there was no previous successful job")
    @Test
    void testDefaultSinceR4FirstRun() {
        Job job = createJob();
        job.setFhirVersion(R4);
        job.setStatus(JobStatus.SUBMITTED);
        when(jobRepository.findByJobUuid(job.getJobUuid())).thenReturn(job);
        when(jobRepository.findByContractNumberEqualsAndStatusInAndStartedByOrderByCompletedAtDesc(anyString(), any(), any())).thenReturn(Collections.emptyList());
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
        oldJob.setStatus(SUCCESSFUL);
        oldJob.setJobUuid(oldJob.getJobUuid() + "-2");
        OffsetDateTime oldJobTime = OffsetDateTime.parse("2021-01-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        oldJob.setCreatedAt(oldJobTime);

        when(jobRepository.findByJobUuid(newJob.getJobUuid())).thenReturn(newJob);
        when(jobRepository.findByContractNumberEqualsAndStatusInAndStartedByOrderByCompletedAtDesc(anyString(), any(), any())).thenReturn(List.of(oldJob));

        cut.preprocess(newJob.getJobUuid());
        assertEquals(oldJobTime, newJob.getSince());
        assertEquals(AB2D, newJob.getSinceSource());
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
        oldJob.setStatus(SUCCESSFUL);
        OffsetDateTime oldJobTime = OffsetDateTime.parse("2021-01-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        oldJob.setCreatedAt(oldJobTime);
        oldJob.setJobUuid(oldJob.getJobUuid() + "-2");

        when(jobRepository.findByJobUuid(newJob.getJobUuid())).thenReturn(newJob);

        cut.preprocess(newJob.getJobUuid());

        verify(jobRepository, never()).findByContractNumberEqualsAndStatusInAndStartedByOrderByCompletedAtDesc(anyString(), any(), any());
        assertEquals(now, newJob.getSince());
        assertEquals(SinceSource.USER, newJob.getSinceSource());
    }

    @Test
    void testDifferentSinceConditions() {
        Job newJob = createJob();
        newJob.setFhirVersion(R4);
        newJob.setStatus(JobStatus.SUBMITTED);
        newJob.setCreatedAt(OffsetDateTime.now());

        ContractDTO contract = new ContractDTO("contractNum", null, null, Contract.ContractType.CLASSIC_TEST);
        newJob.setContractNumber(contract.getContractNumber());

        Job oldJob = createJob();
        oldJob.setStatus(SUCCESSFUL);
        oldJob.setJobUuid(oldJob.getJobUuid() + "-2");
        OffsetDateTime oldJobTime = OffsetDateTime.parse("2021-01-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        oldJob.setCreatedAt(oldJobTime);

        when(jobRepository.findByJobUuid(newJob.getJobUuid())).thenReturn(newJob);

        cut.preprocess(newJob.getJobUuid());

        assertNull(newJob.getSince());
        // No longer allow null contracts so things get flagged as first run now.
        assertEquals(SinceSource.FIRST_RUN, newJob.getSinceSource());

        contract = new ContractDTO("contractNum", null, null, Contract.ContractType.SYNTHEA);

        when(jobRepository.findByContractNumberEqualsAndStatusInAndStartedByOrderByCompletedAtDesc(anyString(), any(), any())).thenReturn(List.of(oldJob));

        cut.preprocess(newJob.getJobUuid());

        assertEquals(oldJob.getCreatedAt(), newJob.getSince());
        assertEquals(AB2D, newJob.getSinceSource());
    }

    @DisplayName("Search for latest successful job with all files downloaded")
    @Test
    void testGetLatestFullySuccessfulJob() {
        // This job has successfully downloaded data files but not error file
        Job job1 = createJob("A", R4, SUCCESSFUL, OffsetDateTime.of(2020, 5, 1, 1, 0, 0, 0, ZoneOffset.UTC));
        job1.addJobOutput(createJobOutput(job1, false, 1));
        job1.addJobOutput(createJobOutput(job1, true, 0));

        // This job has successfully downloaded data all files
        Job job2 = createJob("B", R4, SUCCESSFUL, OffsetDateTime.of(2020, 5, 2, 1, 0, 0, 0, ZoneOffset.UTC));
        job2.addJobOutput(createJobOutput(job2, false, 1));
        job2.addJobOutput(createJobOutput(job2, true, 1));

        // This job has not successfully downloaded data files
        Job job3 = createJob("C", R4, SUCCESSFUL, OffsetDateTime.of(2020, 5, 3, 1, 0, 0, 0, ZoneOffset.UTC));
        job3.addJobOutput(createJobOutput(job3, false, 0));
        job3.addJobOutput(createJobOutput(job3, true, 1));

        // Job has no data files
        Job job4 = createJob("D", R4, SUCCESSFUL, OffsetDateTime.of(2020, 5, 4, 1, 0, 0, 0, ZoneOffset.UTC));

        // This job also has successfully downloaded data files but not error file
        Job job5 = createJob("E", R4, SUCCESSFUL, OffsetDateTime.of(2020, 5, 5, 1, 0, 0, 0, ZoneOffset.UTC));
        job5.addJobOutput(createJobOutput(job5, false, 1));
        job5.addJobOutput(createJobOutput(job5, true, 0));

        Job newJob = createJob("Z", R4, SUBMITTED, OffsetDateTime.of(2020, 7, 1, 1, 0, 0, 0, ZoneOffset.UTC));

        JobPreProcessorImpl impl = (JobPreProcessorImpl) cut;

        assertEquals(impl.getLastSuccessfulJobWithDownloads(List.of(job1, job2, job3, job4)).get().getCreatedAt().getNano(), job4.getCreatedAt().getNano());
        assertEquals(impl.getLastSuccessfulJobWithDownloads(List.of(job1, job2, job3)).get().getCreatedAt().getNano(), job2.getCreatedAt().getNano());
        assertEquals(impl.getLastSuccessfulJobWithDownloads(List.of(job1, job3)).get().getCreatedAt().getNano(), job1.getCreatedAt().getNano());
        assertTrue(impl.getLastSuccessfulJobWithDownloads(List.of(job3)).isEmpty());

        when(jobRepository.findByJobUuid(newJob.getJobUuid())).thenReturn(newJob);
        when(jobRepository.findByContractNumberEqualsAndStatusInAndStartedByOrderByCompletedAtDesc(anyString(), any(), any())).thenReturn(List.of(job1, job2, job3, job4));
        cut.preprocess(newJob.getJobUuid());
        assertEquals(newJob.getSince().getNano(), job4.getCreatedAt().getNano());
        assertEquals(newJob.getSinceSource(), AB2D);
    }

    @DisplayName("Make sure we've found the latest successful job")
    @Test
    void testGetLatestSuccessfulJobWithMany() {
        // This job has successfully downloaded data files but not error file
        Job job1 = createJob("A", R4, SUCCESSFUL, OffsetDateTime.of(2020, 5, 1, 1, 0, 0, 0, ZoneOffset.UTC));
        job1.addJobOutput(createJobOutput(job1, true, 1));

        // This job has successfully downloaded data all files
        Job job2 = createJob("B", R4, SUCCESSFUL, OffsetDateTime.of(2020, 5, 2, 1, 0, 0, 0, ZoneOffset.UTC));
        job2.addJobOutput(createJobOutput(job2, false, 1));
        job2.addJobOutput(createJobOutput(job2, true, 1));

        // This job has not successfully downloaded data files
        Job job3 = createJob("C", R4, SUCCESSFUL, OffsetDateTime.of(2020, 5, 3, 1, 0, 0, 0, ZoneOffset.UTC));
        job3.addJobOutput(createJobOutput(job3, false, 1));
        job3.addJobOutput(createJobOutput(job3, true, 0));

        // Job has no data files
        Job job4 = createJob("D", R4, SUCCESSFUL, OffsetDateTime.of(2020, 5, 4, 1, 0, 0, 0, ZoneOffset.UTC));

        // This job also has successfully downloaded data files but not error file
        Job job5 = createJob("E", R4, SUCCESSFUL, OffsetDateTime.of(2020, 5, 5, 1, 0, 0, 0, ZoneOffset.UTC));
        job5.addJobOutput(createJobOutput(job5, false, 1));
        job5.addJobOutput(createJobOutput(job5, true, 0));

        Job newJob = createJob("Z", R4, SUBMITTED, OffsetDateTime.of(2020, 7, 1, 1, 0, 0, 0, ZoneOffset.UTC));

        JobPreProcessorImpl impl = (JobPreProcessorImpl) cut;

        assertEquals(impl.getLastSuccessfulJobWithDownloads(List.of(job1, job2)).get().getCreatedAt().getNano(), job2.getCreatedAt().getNano());
        assertEquals(impl.getLastSuccessfulJobWithDownloads(List.of(job1, job2, job3)).get().getCreatedAt().getNano(), job3.getCreatedAt().getNano());
        assertEquals(impl.getLastSuccessfulJobWithDownloads(List.of(job1, job2, job3, job4)).get().getCreatedAt().getNano(), job4.getCreatedAt().getNano());
    }

    @Test
    void testDownloadedAll() {
        Job job = new Job();
        // Error file that was downloaded
        JobOutput jo1 = createJobOutput(job, true, 1);
        // Error file that was not downloaded
        JobOutput jo2 = createJobOutput(job, true, 0);
        // Data file that was downloaded
        JobOutput jo3 = createJobOutput(job, false, 1);
        // Data file that was not downloaded - anything that includes this should return false
        JobOutput jo4 = createJobOutput(job, false, 0);

        JobPreProcessorImpl impl = (JobPreProcessorImpl) cut;
        // Start with null or empty results
        assertTrue(impl.downloadedAll(null));
        assertTrue(impl.downloadedAll(Collections.emptyList()));

        // Try each individual
        assertTrue(impl.downloadedAll(List.of(jo1)));
        assertTrue(impl.downloadedAll(List.of(jo2)));
        assertTrue(impl.downloadedAll(List.of(jo3)));
        assertFalse(impl.downloadedAll(List.of(jo4)));

        // Try combinations
        assertTrue(impl.downloadedAll(List.of(jo1, jo2, jo3)));
        assertFalse(impl.downloadedAll(List.of(jo1, jo2, jo3, jo4)));
        assertFalse(impl.downloadedAll(List.of(jo1, jo4)));
        assertFalse(impl.downloadedAll(List.of(jo2, jo3, jo4)));
    }

    private JobOutput createJobOutput(Job job, boolean error, int downloaded) {
        JobOutput output = new JobOutput();
        output.setError(error);
        output.setFilePath("/tmp/" + job.getJobUuid() + (int) (Math.random() * 1000));
        output.setChecksum("" + (int) (Math.random() * 10000000));
        output.setFileLength((long) (Math.random() * 10000000));
        output.setDownloaded(downloaded);
        output.setFhirResourceType(EOB);
        output.setId((long) (Math.random() * 10000000));
        return output;
    }

    private Job createJob(String uuid, FhirVersion version, JobStatus status, OffsetDateTime created) {
        Job job = new Job();
        job.setJobUuid(uuid);
        job.setStatusMessage("0%");
        job.setFhirVersion(version);
        job.setStatus(status);
        job.setCreatedAt(created);
        job.setContractNumber(contract.getContractNumber());

        return job;

    }

    // Not first run, since supplied

    private Job createJob() {
        Job job = new Job();
        job.setJobUuid(JOB_UUID);
        job.setStatusMessage("0%");
        job.setFhirVersion(STU3);
        job.setContractNumber(contract.getContractNumber());

        return job;
    }
}