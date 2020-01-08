package gov.cms.ab2d.worker.stuckjob;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.JobRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelStuckJobsProcessorTest {
    //class under test
    private CancelStuckJobsProcessor cut;

    private String jobUuid = "6d08bf08-f926-4e19-8d89-ad67ef89f17e";

    @Mock   JobRepository mockJobRepo;
    @Captor ArgumentCaptor<Job> captor;

    private List<Job> jobs = new ArrayList<>();

    @BeforeEach
    void setUp() {
        cut = new CancelStuckJobsProcessorImpl(mockJobRepo);
        ReflectionTestUtils.setField(cut, "cancelThreshold", 6);

        jobs.add(createStuckJob(7));
        when(mockJobRepo.findStuckJobs(Mockito.any())).thenReturn(jobs);
    }


    @Test
    void whenAStuckJobIsFound_ShouldCancelIt() {
        cut.process();

        verify(mockJobRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus(), is(JobStatus.CANCELLED));
    }

    @Test
    void whenMultipleStuckJobsAreFound_ShouldCancelEachOfThem() {
        jobs.add(createStuckJob(8));
        jobs.add(createStuckJob(9));

        cut.process();

        verify(mockJobRepo, times(3)).save(captor.capture());

        final List<Job> capturedJobs = captor.getAllValues();

        assertThat(capturedJobs.size(), is(3));
        capturedJobs.forEach( job -> {
                assertThat(job.getStatus(), is(JobStatus.CANCELLED));
        });

    }

    private Job createStuckJob(final int hoursAgo) {
        Job job = new Job();
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setCreatedAt(OffsetDateTime.now().minusHours(hoursAgo));
        return job;
    }
}