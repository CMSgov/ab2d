package gov.cms.ab2d.worker.processor.eob;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.util.EventUtils;
import gov.cms.ab2d.eventlogger.LogManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static gov.cms.ab2d.common.model.JobStatus.IN_PROGRESS;
import static gov.cms.ab2d.common.model.JobStatus.SUBMITTED;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobPreProcessorImpl implements JobPreProcessor {

    private final JobRepository jobRepository;
    private final LogManager eventLogger;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public Job preprocess(Job job) {

        String jobUuid = job.getJobUuid();

        // validate status is SUBMITTED
        if (!SUBMITTED.equals(job.getStatus())) {
            final String errMsg = String.format("Job %s is not in %s status", jobUuid, SUBMITTED);
            log.error("Job is not in submitted status");
            throw new IllegalArgumentException(errMsg);
        }
        eventLogger.log(EventUtils.getJobChangeEvent(job, IN_PROGRESS, "Job in progress"));

        job.setStatus(IN_PROGRESS);
        job.setStatusMessage(null);
        return jobRepository.save(job);
    }
}
