package gov.cms.ab2d.worker.stuckjob;

import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.repository.JobRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
    private final CoverageV3Service coverageV3Service;

    public CancelStuckJobsProcessorImpl(
            JobRepository jobRepository,
            SQSEventClient eventLogger,
            @Value("${stuck.job.cancel.threshold}") int cancelThreshold,
            CoverageV3Service coverageV3Service) {
        this.jobRepository = jobRepository;
        this.eventLogger = eventLogger;
        this.cancelThreshold = cancelThreshold;
        this.coverageV3Service = coverageV3Service;
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
            if (stuckJob.getFhirVersion() == FhirVersion.R4V3) {
                coverageV3Service.deleteAggregatedAttributionTable(stuckJob.getContractNumber(), Optional.of(stuckJob.getJobUuid()));
            }
        }
    }

}
