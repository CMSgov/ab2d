package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.JobStatusChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShutDownServiceImpl implements ShutDownService {

    private final JobRepository jobRepository;
    private final LogManager eventLogger;

    @Override
    @Transactional
    public void resetInProgressJobs(List<String> activeJobs) {
        log.info("Reset jobs : {} to SUBMITTED status", activeJobs);
        try {
            for (String jobString : activeJobs) {
                Job job = jobRepository.findByJobUuid(jobString);
                eventLogger.log(new JobStatusChangeEvent(
                        job.getUser() == null ? null : job.getUser().getUsername(),
                        job.getJobUuid(),
                        job.getStatus() == null ? null : job.getStatus().name(),
                        JobStatus.SUBMITTED.name(), "Job status reset to SUBMITTED on shutdown"));
            }

            jobRepository.resetJobsToSubmittedStatus(activeJobs);
        } catch (Exception e) {
            log.error("Error while doing house cleaning during shutdown ", e);
            // do nothing
        }
    }
}
