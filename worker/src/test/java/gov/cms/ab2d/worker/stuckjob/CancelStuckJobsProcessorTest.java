package gov.cms.ab2d.worker.stuckjob;

import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.job.repository.JobRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;


import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelStuckJobsProcessorTest {
    //class under test
    private CancelStuckJobsProcessor cut;

    @Mock
    JobRepository mockJobRepo;

    @Mock
    private SQSEventClient eventLogger;

    @Captor
    private ArgumentCaptor<Job> captor;

    private final List<Job> jobs = new ArrayList<>();

    @BeforeEach
    void setUp() {
        cut = new CancelStuckJobsProcessorImpl(mockJobRepo, eventLogger, 36);
        ReflectionTestUtils.setField(cut, "cancelThreshold", 6);

        jobs.add(createStuckJob(7));
        when(mockJobRepo.findStuckJobs(Mockito.any())).thenReturn(jobs);
    }


    @Test
    void whenAStuckJobIsFound_ShouldCancelIt() {
        cut.process();

        verify(mockJobRepo).save(captor.capture());
        assertEquals(JobStatus.CANCELLED, captor.getValue().getStatus());
    }

    @Test
    void whenMultipleStuckJobsAreFound_ShouldCancelEachOfThem() {
        jobs.add(createStuckJob(8));
        jobs.add(createStuckJob(9));

        cut.process();

        verify(mockJobRepo, times(3)).save(captor.capture());

        final List<Job> capturedJobs = captor.getAllValues();

        assertEquals(3, capturedJobs.size());
        capturedJobs.forEach( job -> assertEquals(JobStatus.CANCELLED, job.getStatus()));

    }

    private Job createStuckJob(final int hoursAgo) {
        Job job = new Job();
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setCreatedAt(OffsetDateTime.now().minusHours(hoursAgo));
        job.setFhirVersion(STU3);
        return job;
    }
}