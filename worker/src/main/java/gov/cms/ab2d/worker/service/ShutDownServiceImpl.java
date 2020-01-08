package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.repository.JobRepository;
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

    @Override
    @Transactional
    public void resetInProgressJobs(List<String> activeJobs) {
        log.info("Reset jobs : {} to SUBMITTED status", activeJobs);
        try {
            jobRepository.resetJobsToSubmittedStatus(activeJobs);
        } catch (Exception e) {
            log.error("Error while doing house cleaning during shutdown ", e);
            // do nothing
        }
    }
}
