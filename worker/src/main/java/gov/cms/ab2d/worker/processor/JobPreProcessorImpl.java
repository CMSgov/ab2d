package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.common.model.SinceSource;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobOutput;
import gov.cms.ab2d.job.model.JobStartedBy;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriverException;
import gov.cms.ab2d.worker.service.ContractWorkerClient;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import static gov.cms.ab2d.eventclient.config.Ab2dEnvironment.PUBLIC_LIST;
import static gov.cms.ab2d.eventclient.events.SlackEvents.EOB_JOB_COVERAGE_ISSUE;
import static gov.cms.ab2d.eventclient.events.SlackEvents.EOB_JOB_STARTED;
import static gov.cms.ab2d.eventclient.events.SlackEvents.EOB_JOB_FAILURE;
import static gov.cms.ab2d.job.model.JobStatus.FAILED;
import static gov.cms.ab2d.job.model.JobStatus.IN_PROGRESS;
import static gov.cms.ab2d.job.model.JobStatus.SUBMITTED;
import static gov.cms.ab2d.job.model.JobStatus.SUCCESSFUL;

@Slf4j
@Component
//java:S2142: "InterruptedException" should not be ignored
//java:S3655: False flag. Complaining about not checking for Optional#isPresent() when it is checked
@SuppressWarnings({"java:S2142", "java:S2583"})
public class JobPreProcessorImpl implements JobPreProcessor {

    private final ContractWorkerClient contractWorkerClient;
    private final JobRepository jobRepository;
    private final SQSEventClient eventLogger;
    private final CoverageDriver coverageDriver;

    public JobPreProcessorImpl(ContractWorkerClient contractWorkerClient, JobRepository jobRepository, SQSEventClient logManager,
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

        if (job.getFhirVersion() == FhirVersion.STU3 && job.getUntil() != null) {
            log.warn("JobPreProcessorImpl > preprocess: job FAILED because the _until parameter is only available with version 2 (FHIR R4).");

            eventLogger.logAndAlert(job.buildJobStatusChangeEvent(FAILED, EOB_JOB_FAILURE + " Job " + jobUuid
                    + "failed because the _until parameter is only available with version 2 (FHIR R4)"), PUBLIC_LIST);

            job.setStatus(FAILED);
            job.setStatusMessage("failed because the _until parameter is only available with version 2 (FHIR R4).");

            jobRepository.save(job);
            return job;
        }

        if (job.getSince() != null && job.getUntil() != null
                && job.getUntil().toInstant().isBefore(job.getSince().toInstant())) {
            log.warn("JobPreProcessorImpl > preprocess: job FAILED because the _until is before _since.");

            eventLogger.logAndAlert(job.buildJobStatusChangeEvent(FAILED, EOB_JOB_FAILURE + " Job " + jobUuid
                    + "failed because the _until is before _since."), PUBLIC_LIST);

            job.setStatus(FAILED);
            job.setStatusMessage("failed because the _until is before _since.");

            jobRepository.save(job);
            return job;
        }

        ContractDTO contract = contractWorkerClient.getContractByContractNumber(job.getContractNumber());
        if (contract == null) {
            throw new IllegalArgumentException("A job must always have a contract.");
        }

        if (contract.getAttestedOn() == null) {
            log.warn("JobPreProcessorImpl > preprocess: job FAILED because the contract attestation date is null.");

            eventLogger.logAndAlert(job.buildJobStatusChangeEvent(FAILED, EOB_JOB_FAILURE + " Job " + jobUuid
                + "failed for contract: " + contract.getContractNumber() + " because contract attestation date is null"), PUBLIC_LIST);

            job.setStatus(FAILED);
            job.setStatusMessage("failed because contract attestation date is null.");

            jobRepository.save(job);
            return job;
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

            eventLogger.logAndAlert(job.buildJobStatusChangeEvent(IN_PROGRESS, getStatusString(job)), PUBLIC_LIST);

            job.setStatus(IN_PROGRESS);
            job.setStatusMessage(null);

            job = jobRepository.save(job);

        } catch (CoverageDriverException coverageDriverException) {
            eventLogger.logAndAlert(job.buildJobStatusChangeEvent(FAILED, EOB_JOB_COVERAGE_ISSUE + " Job for "
                    + contract.getContractNumber() + " in progress"), PUBLIC_LIST);

            job.setStatus(FAILED);
            job.setStatusMessage("could not pull coverage information for contract");

            job = jobRepository.save(job);
        } catch (InterruptedException ie) {
            throw new RuntimeException("could not determine whether coverage metadata was up to date", ie);
        }

        return job;
    }

    String getStatusString(Job job) {
        if (job == null) {
            return "";
        }
        String contractNum = job.getContractNumber() == null ? "(unknown)" : job.getContractNumber();
        String statusString = String.format("%s for %s in progress", EOB_JOB_STARTED, contractNum);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        if (job.getSince() != null) {
            statusString += " (since date: " + job.getSince().format(formatter) + ")";
        }
        if (job.getUntil() != null) {
            statusString += " (until date: " + job.getUntil().format(formatter) + ")";
        }
        return statusString;
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
                .filter(o -> o.getDownloaded() == 0)
                // Determine if there are any left
                .findAny().isEmpty();
    }
}
