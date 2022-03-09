package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.JobStartedBy;
import gov.cms.ab2d.common.model.SinceSource;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.util.EventUtils;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriverException;
import gov.cms.ab2d.worker.service.ContractWorkerClient;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import static gov.cms.ab2d.common.model.JobStatus.FAILED;
import static gov.cms.ab2d.common.model.JobStatus.IN_PROGRESS;
import static gov.cms.ab2d.common.model.JobStatus.SUBMITTED;
import static gov.cms.ab2d.common.model.JobStatus.SUCCESSFUL;
import static gov.cms.ab2d.eventlogger.Ab2dEnvironment.PUBLIC_LIST;
import static gov.cms.ab2d.eventlogger.events.SlackEvents.EOB_JOB_COVERAGE_ISSUE;
import static gov.cms.ab2d.eventlogger.events.SlackEvents.EOB_JOB_STARTED;

@Slf4j
@Component
@SuppressWarnings("java:S2142") //java:S2142: "InterruptedException" should not be ignored
public class JobPreProcessorImpl implements JobPreProcessor {

    private final ContractWorkerClient contractWorkerClient;
    private final JobRepository jobRepository;
    private final LogManager eventLogger;
    private final CoverageDriver coverageDriver;

    public JobPreProcessorImpl(ContractWorkerClient contractWorkerClient, JobRepository jobRepository, LogManager logManager,
                               CoverageDriver coverageDriver) {
        this.contractWorkerClient = contractWorkerClient;
        this.jobRepository = jobRepository;
        this.eventLogger = logManager;
        this.coverageDriver = coverageDriver;
    }

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

        ContractDTO contract = contractWorkerClient.getContractByContractNumber(job.getContractNumber());
        if (contract == null) {
            throw new IllegalArgumentException("A job must always have a contract.");
        }
        Optional<OffsetDateTime> sinceValue = Optional.ofNullable(job.getSince());
        if (sinceValue.isPresent()) {
            // If the user provided a 'since' value
            job.setSinceSource(SinceSource.USER);
            jobRepository.save(job);
        } else if (job.getFhirVersion().supportDefaultSince() && !contract.hasDateIssue()) {
            // If the user did not, but this version supports a default 'since', populate it
            job = updateSinceTime(job, contract);
            jobRepository.save(job);
        }

        try {
            if (!coverageDriver.isCoverageAvailable(job, contract)) {
                log.info("coverage metadata is not up to date so job will not be started");
                return job;
            }

            eventLogger.logAndAlert(EventUtils.getJobChangeEvent(job, IN_PROGRESS, EOB_JOB_STARTED + " for "
                    + contract.getContractNumber() + " in progress"), PUBLIC_LIST);

            job.setStatus(IN_PROGRESS);
            job.setStatusMessage(null);

            job = jobRepository.save(job);

        } catch (CoverageDriverException coverageDriverException) {
            eventLogger.logAndAlert(EventUtils.getJobChangeEvent(job, FAILED, EOB_JOB_COVERAGE_ISSUE + " Job for "
                    + contract.getContractNumber() + " in progress"), PUBLIC_LIST);

            job.setStatus(FAILED);
            job.setStatusMessage("could not pull coverage information for contract");

            job = jobRepository.save(job);
        } catch (InterruptedException ie) {
            throw new RuntimeException("could not determine whether coverage metadata was up to date", ie);
        }

        return job;
    }

    /**
     * Update the 'since' logic if the user has not supplied one. We pick the date the last job was successfully
     * run by the PDP (ignoring AB2D run jobs). If no job has every been successfully run, we default to a null
     * since date.
     *
     * @param job - The job object to update (although not save)
     * @return - the job with the updated since date and auto since source
     */
    Job updateSinceTime(Job job, ContractDTO contract) {
        List<Job> successfulJobs = jobRepository.findByContractNumberEqualsAndStatusInAndStartedByOrderByCompletedAtDesc(
                contract.getContractNumber(), List.of(SUCCESSFUL), JobStartedBy.PDP);

        // Get time of last successful job for that organization
        Optional<Job> successfulJob = getLastSuccessfulJobWithDownloads(successfulJobs);
        if (successfulJob.isPresent()) {
            // If there was a successful job, set the since time to the last submitted job date
            job.setSince(successfulJob.get().getCreatedAt());
            job.setSinceSource(SinceSource.AB2D);
        } else {
            // If there was not, this mean this is the first time the job was run
            job.setSinceSource(SinceSource.FIRST_RUN);
        }
        return job;
    }

    /**
     * While we are looking for previously successful jobs to use it as a since date, we have to be careful
     * to only include jobs whose data files have been downloaded
     *
     * @param successfulJobs - the list of historical successful jobs
     * @return - the last successful job
     */
    Optional<Job> getLastSuccessfulJobWithDownloads(List<Job> successfulJobs) {
        Comparator<Job> comparator = Comparator.comparing(Job::getCreatedAt);

        List<Job> sortedFilteredlist = successfulJobs.stream()
                .filter(j -> downloadedAll(j.getJobOutputs()))
                .sorted(comparator)
                .collect(Collectors.toList());

        if (sortedFilteredlist.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(sortedFilteredlist.get(sortedFilteredlist.size() - 1));
    }

    /**
     * Return true if all data files have been downloaded for the job
     *
     * @param outputs - the data outputs related to the job
     * @return true if all non error files have been downloaded, false if any data files were not downloaded
     */
    boolean downloadedAll(List<JobOutput> outputs) {
        if (outputs == null) {
            return true;
        }
        return outputs.stream()
                // Remove any error files from the consideration
                .filter(o -> !o.getError())
                // Remove any that has been downloaded
                .filter(o -> !o.getDownloaded())
                // Determine if there are any left
                .findAny().isEmpty();
    }
}
