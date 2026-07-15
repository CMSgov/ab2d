package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.job.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;

/**
 * Polls the AB2D job status once per chunk and, if the job has been cancelled, terminate
 * the worker step at the next chunk boundary
 */
@Slf4j
public class JobCancellationChunkListener implements ChunkListener {

    private final JobRepository jobRepository;
    private final String jobUuid;

    public JobCancellationChunkListener(JobRepository jobRepository, String jobUuid) {
        this.jobRepository = jobRepository;
        this.jobUuid = jobUuid;
    }

    @Override
    public void beforeChunk(ChunkContext context) {
        if (jobRepository.getJobStatusOfJob(jobUuid) == JobStatus.CANCELLED) {
            log.warn("job {} was cancelled - terminating worker step at chunk boundary", jobUuid);
            context.getStepContext().getStepExecution().setTerminateOnly();
        }
    }
}
