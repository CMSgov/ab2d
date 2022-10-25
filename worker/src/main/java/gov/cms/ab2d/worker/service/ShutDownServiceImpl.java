package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.repository.JobRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import static gov.cms.ab2d.eventclient.config.Ab2dEnvironment.PUBLIC_LIST;
import static gov.cms.ab2d.eventclient.events.SlackEvents.EOB_JOB_RESUBMITTED;
import static gov.cms.ab2d.job.model.JobStatus.SUBMITTED;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShutDownServiceImpl implements ShutDownService {

    private final JobRepository jobRepository;
    private final SQSEventClient eventLogger;

    @Override
    @Transactional
    public void resetInProgressJobs(List<String> activeJobs) {
        log.info("Reset jobs : {} to SUBMITTED status", activeJobs);
        try {
            for (String jobString : activeJobs) {
                Job job = jobRepository.findByJobUuid(jobString);
                eventLogger.logAndAlert(job.buildJobStatusChangeEvent(SUBMITTED,
                                EOB_JOB_RESUBMITTED + " Job status reset to SUBMITTED on shutdown"),
                                            PUBLIC_LIST);
            }

            jobRepository.resetJobsToSubmittedStatus(activeJobs);
        } catch (Exception e) {
            log.error("Error while doing house cleaning during shutdown ", e);
            // do nothing
        }
    }
}
