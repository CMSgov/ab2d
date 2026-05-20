package gov.cms.ab2d.worker.stuckjob;

import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.repository.JobRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import static gov.cms.ab2d.eventclient.config.Ab2dEnvironment.PUBLIC_LIST;
import static gov.cms.ab2d.eventclient.events.SlackEvents.EOB_JOB_CANCELLED;
import static gov.cms.ab2d.job.model.JobStatus.CANCELLED;

@Slf4j
@Component
public class CancelStuckJobsProcessorImpl implements CancelStuckJobsProcessor {

    private final JobRepository jobRepository;
    private final SQSEventClient eventLogger;
    private final int cancelThreshold;

    public CancelStuckJobsProcessorImpl(JobRepository jobRepository, SQSEventClient eventLogger,
                                        @Value("${stuck.job.cancel.threshold}") int cancelThreshold) {
        this.jobRepository = jobRepository;
        this.eventLogger = eventLogger;
        this.cancelThreshold = cancelThreshold;
    }

    @Override
    @Transactional
    public void process() {

        final OffsetDateTime cutOffTime = OffsetDateTime.now().minusHours(cancelThreshold);

        final List<Job> stuckJobs = jobRepository.findStuckJobs(cutOffTime);
        log.info("Found {} jobs that appears to be stuck", stuckJobs.size());

        for (Job stuckJob : stuckJobs) {
            log.warn("Cancelling job - uuid:{} - created at:{} - status :{} - completed_at {}",
                            stuckJob.getJobUuid(), stuckJob.getCreatedAt(), stuckJob.getStatus(), stuckJob.getCompletedAt());
            eventLogger.logAndAlert(stuckJob.buildJobStatusChangeEvent(CANCELLED, EOB_JOB_CANCELLED + " Cancelling stuck job"),
                    PUBLIC_LIST);
            stuckJob.setStatus(CANCELLED);
            jobRepository.save(stuckJob);
        }
    }
}
