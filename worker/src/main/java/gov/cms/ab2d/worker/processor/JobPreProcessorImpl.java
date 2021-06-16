package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.util.EventUtils;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriverException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static gov.cms.ab2d.common.model.JobStatus.*;

@AllArgsConstructor
@Slf4j
@Component
public class JobPreProcessorImpl implements JobPreProcessor {

    private final JobRepository jobRepository;
    private final LogManager eventLogger;
    private final CoverageDriver coverageDriver;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public Job preprocess(String jobUuid) {

        Job job = jobRepository.findByJobUuid(jobUuid);
        if (job == null) {
            log.error("Job was not found");
            throw new IllegalArgumentException("Job " + jobUuid + " was not found");
        }

        // validate status is SUBMITTED
        if (!SUBMITTED.equals(job.getStatus())) {
            final String errMsg = String.format("Job %s is not in %s status", jobUuid, SUBMITTED);
            log.error("Job is not in submitted status");
            throw new IllegalArgumentException(errMsg);
        }

        try {
            if (!coverageDriver.isCoverageAvailable(job)) {
                log.info("coverage metadata is not up to date so job will not be started");
                return job;
            }

            eventLogger.log(EventUtils.getJobChangeEvent(job, IN_PROGRESS, "Job in progress"));

            job.setStatus(IN_PROGRESS);
            job.setStatusMessage(null);

            job = jobRepository.save(job);

        } catch (CoverageDriverException coverageDriverException) {
            eventLogger.log(EventUtils.getJobChangeEvent(job, FAILED, "Job in progress"));

            job.setStatus(FAILED);
            job.setStatusMessage("could not pull coverage information for contract");

            job = jobRepository.save(job);
        } catch (InterruptedException ie) {
            throw new RuntimeException("could not determine whether coverage metadata was up to date", ie);
        }


        return job;
    }
}
