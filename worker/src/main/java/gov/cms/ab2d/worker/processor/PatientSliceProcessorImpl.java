package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.OptOutRepository;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import gov.cms.ab2d.worker.processor.domainmodel.ContractData;
import gov.cms.ab2d.worker.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

import static gov.cms.ab2d.common.model.JobStatus.CANCELLED;
import static gov.cms.ab2d.common.model.JobStatus.FAILED;
import static gov.cms.ab2d.common.model.JobStatus.IN_PROGRESS;
import static gov.cms.ab2d.common.util.Constants.EOB;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientSliceProcessorImpl implements PatientSliceProcessor {
    private static final String OUTPUT_FILE_SUFFIX = ".ndjson";
    private static final String ERROR_FILE_SUFFIX = "_error.ndjson";

    @Value("${cancellation.check.frequency:10}")
    private int cancellationCheckFrequency;

    @Value("${report.progress.db.frequency:100}")
    private int reportProgressDbFrequency;

    @Value("${report.progress.log.frequency:100}")
    private int reportProgressLogFrequency;

    private final FileService fileService;
    private final JobRepository jobRepository;
    private final JobOutputRepository jobOutputRepository;
    private final OptOutRepository optOutRepository;
    private final PatientClaimsProcessor patientClaimsProcessor;

    @Override
    @Async("pcpThreadPool")
    public CompletableFuture<Void> process(Map.Entry<Integer, List<PatientDTO>> slice, ContractData contractData) {
        log.info("Slice [{}] has [{}] patients ", slice.getKey(), slice.getValue().size());
        var startedAt = Instant.now();

        var key = slice.getKey();

        var contractNumber = contractData.getContract().getContractNumber();
        var outputFilename = createFileName(contractNumber, key, OUTPUT_FILE_SUFFIX);
        var errorFileName = createFileName(contractNumber, key, ERROR_FILE_SUFFIX);

        var outputDir = contractData.getOutputDir();
        var outputFile = fileService.createOrReplaceFile(outputDir, outputFilename);
        var errorFile = fileService.createOrReplaceFile(outputDir, errorFileName);

        final List<PatientDTO> patientsSlice = slice.getValue();
        try {
            int errorCount = processPatients(contractData, outputFile, errorFile, patientsSlice);

            var jobOutputs = new ArrayList<JobOutput>();
            if (errorCount < patientsSlice.size()) {
                jobOutputs.add(createJobOutput(outputFile, false));
            }
            if (errorCount > 0) {
                log.warn("Encountered {} errors during job processing", errorCount);
                jobOutputs.add(createJobOutput(errorFile, true));
            }

            var jobUuid = contractData.getProgressTracker().getJobUuid();
            var job = jobRepository.findByJobUuid(jobUuid);
            jobOutputs.forEach(jobOutput -> job.addJobOutput(jobOutput));
            jobOutputRepository.saveAll(jobOutputs);
        } catch (RuntimeException e) {
            log.error("Unexpected exception ", e);
            var jobUuid = contractData.getProgressTracker().getJobUuid();
            var failureMessage = ExceptionUtils.getRootCauseMessage(e);
            jobRepository.saveJobFailure(jobUuid, failureMessage);
//            jobRepository.saveJobFailure(jobUuid, failureMessage, OffsetDateTime.now());
        }

        logTimeTaken(slice.getKey(), startedAt);
        return CompletableFuture.completedFuture(null);
    }

    private int processPatients(ContractData contractData, Path outputFile, Path errorFile, List<PatientDTO> patientsSlice) {
        int errorCount = 0;
        int recordsProcessedCount = 0;

        for (PatientDTO patient : patientsSlice) {
            ++recordsProcessedCount;

            if (recordsProcessedCount % cancellationCheckFrequency == 0) {
                final boolean isJobStillInProgress = isJobStillInProgress(contractData);
                if (!isJobStillInProgress) {
                    break;
                }

                // Needs more work
//                updateProgress(contractData.getProgressTracker());
            }

            var patientId = patient.getPatientId();

            if (isOptOutPatient(patientId)) {
                // this patient has opted out. skip patient record.
                continue;
            }

            errorCount += patientClaimsProcessor.processSync(patientId, new ReentrantLock(), outputFile, errorFile);
        }

        return errorCount;
    }



    private boolean isJobStillInProgress(ContractData contractData) {
        var jobUuid = contractData.getProgressTracker().getJobUuid();
        var jobStatus = jobRepository.findJobStatus(jobUuid);
        if (IN_PROGRESS.equals(jobStatus)) {
            return true;
        }

        var mesg = "Job [" + jobUuid + "] is no longer in progress";
        log.warn("{}. It is in [{}] status", mesg, jobStatus);

        if (CANCELLED.equals(jobStatus)) {
            var errMsg = "Job is in cancelled status. Stopping processing";
            log.warn("{}", errMsg);
        } else if (FAILED.equals(jobStatus)) {
            var errMsg = "Job is in failed status. Stopping processing";
            log.warn("{}", errMsg);
        }

        return false;
    }

    // Not ready
//    private void updateProgress(ProgressTracker progressTracker) {
//        if (progressTracker.isTimeToUpdateDatabase(reportProgressDbFrequency)) {
//            final int percentageCompleted = progressTracker.getPercentageCompleted();
//
//            if (percentageCompleted > progressTracker.getLastUpdatedPercentage()) {
//                jobRepository.updatePercentageCompleted(progressTracker.getJobUuid(), percentageCompleted);
//                progressTracker.setLastUpdatedPercentage(percentageCompleted);
//            }
//        }
//
//        var processedCount = progressTracker.getProcessedCount();
//        if (progressTracker.isTimeToLog(reportProgressLogFrequency)) {
//            progressTracker.setLastLogUpdateCount(processedCount);
//
//            var totalCount = progressTracker.getTotalCount();
//            var percentageCompleted = progressTracker.getPercentageCompleted();
//            log.info("[{}/{}] records processed = [{}% completed]", processedCount, totalCount, percentageCompleted);
//        }
//    }



    private boolean isOptOutPatient(String patientId) {

        final List<OptOut> optOuts = optOutRepository.findByHicn(patientId);
        if (optOuts.isEmpty()) {
            // No opt-out record found for this patient - Opt-In by default.
            return false;
        }

        // opt-out record has an effective date.
        // if any of the opt-out records for a patient is effective as of today or earlier, the patient has opted-out
        final LocalDate tomorrow = LocalDate.now().plusDays(1);
        return optOuts.stream()
                .anyMatch(optOut -> optOut.getEffectiveDate().isBefore(tomorrow));
    }

    private String createFileName(String contractNumber, Integer key, String outputFileSuffix) {
        return new StringBuilder()
                .append(contractNumber)
                .append("_")
                .append(StringUtils.leftPad(key.toString(), 5, '0'))
                .append(outputFileSuffix)
                .toString();
    }

    private JobOutput createJobOutput(Path outputFile, boolean isError) {
        JobOutput jobOutput = new JobOutput();
        jobOutput.setFilePath(outputFile.getFileName().toString());
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setError(isError);
        return jobOutput;
    }

    private void logTimeTaken(int sliceSno, Instant start) {
        var timeTaken = Duration.between(start, Instant.now()).toSeconds();
        log.info("Slice [{}] completed in [{}] seconds", sliceSno, timeTaken);
    }
}
