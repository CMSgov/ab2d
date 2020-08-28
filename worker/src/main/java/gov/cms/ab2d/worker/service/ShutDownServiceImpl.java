package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.util.EventUtils;
import gov.cms.ab2d.eventlogger.LogManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static gov.cms.ab2d.common.model.JobStatus.SUBMITTED;

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
                eventLogger.log(EventUtils.getJobChangeEvent(job, SUBMITTED, "Job status reset to SUBMITTED on shutdown"));
            }

            jobRepository.resetJobsToSubmittedStatus(activeJobs);
        } catch (Exception e) {
            log.error("Error while doing house cleaning during shutdown ", e);
            // do nothing
        }
    }
}
