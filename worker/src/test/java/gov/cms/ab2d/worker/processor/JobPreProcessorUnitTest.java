package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.worker.processor.JobPreProcessor;
import gov.cms.ab2d.worker.processor.JobPreProcessorImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class JobPreProcessorUnitTest {
    // class under test
    private JobPreProcessor cut;

    private String jobUuid = "6d08bf08-f926-4e19-8d89-ad67ef89f17e";

    @Mock
    private JobRepository jobRepository;


    private Job job;

    @BeforeEach
    void setUp() {
        cut = new JobPreProcessorImpl(jobRepository);
        job = createJob();
    }


    @Test
    @DisplayName("Throws exception when given a JobUuid for which a job that does not exist")
    void whenGivenJobUuidDoesNotHaveMatchingJobRecord_ThrowsException() {
        var exceptionThrown = assertThrows(
                IllegalArgumentException.class,
                () -> cut.preprocess(jobUuid));

        assertThat(exceptionThrown.getMessage(), equalTo(String.format("Job %s was not found", jobUuid)));
    }


    @Test
    @DisplayName("Throws exception when the job for the given JobUuid is not in submitted status")
    void whenTheJobForTheGivenJobUuidIsNotInSubmittedStatus_ThrowsException() {
        when(jobRepository.findByJobUuid(anyString())).thenReturn(job);

        var exceptionThrown = assertThrows(
                IllegalArgumentException.class,
                () -> cut.preprocess(jobUuid));

        assertThat(exceptionThrown.getMessage(), equalTo(String.format("Job %s is not in SUBMITTED status", jobUuid)));
    }


    @Test
    @DisplayName("When a job is in submitted status, it will be moved to IN_PROGRESS status during pre-processing")
    void whenJobIsInSubmittedStatus_ThenJobShouldBePutInProgress() {
        job.setStatus(JobStatus.SUBMITTED);

        when(jobRepository.findByJobUuid(anyString())).thenReturn(job);
        when(jobRepository.save(Mockito.any())).thenReturn(job);

        var processedJob = cut.preprocess(jobUuid);

        assertThat(processedJob.getStatus(), is(JobStatus.IN_PROGRESS));
        verify(jobRepository).findByJobUuid(Mockito.any());
        verify(jobRepository).save(Mockito.any());
    }


    private Job createJob() {
        Job job = new Job();
        job.setJobUuid(jobUuid);
        job.setStatusMessage("0%");
        return job;
    }


}