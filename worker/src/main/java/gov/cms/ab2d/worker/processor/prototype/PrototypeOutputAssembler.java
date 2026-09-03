package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.aggregator.FileOutputType;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobOutput;
import gov.cms.ab2d.job.repository.JobOutputRepository;
import gov.cms.ab2d.worker.config.SearchConfig;
import gov.cms.ab2d.worker.processor.StreamOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles a completed prototype job's final, downloadable output.
 * The corollary to the Aggregator, the assembler basically does the same job. But this version
 * picks files from multiple potential candidates, tolerates unfinished prior assembly, and does so
 * idempotently.
 *
 */
@Slf4j
@Component
public class PrototypeOutputAssembler {

    private static final int BYTES_PER_MB = 1024 * 1024;

    private final SearchConfig searchConfig;
    private final JobOutputRepository jobOutputRepository;
    private final PrototypeBatchMetadataRepository batchMeta;
    private final CrashInjector crashInjector;

    public PrototypeOutputAssembler(SearchConfig searchConfig, JobOutputRepository jobOutputRepository,
                                    PrototypeBatchMetadataRepository batchMeta, CrashInjector crashInjector) {
        this.searchConfig = searchConfig;
        this.jobOutputRepository = jobOutputRepository;
        this.batchMeta = batchMeta;
        this.crashInjector = crashInjector;
    }

    /**
     * Promotes winning partition files and then concatenates the winners into finished 200mb files
     * Creates a JobOutput per finished file.
     *
     * @throws IOException If a winning file is missing, the job is a failure and cannot be completed.
     */
    public void assemble(Job job, String jobUuid, String contractNumber) throws IOException {
        // existence of JobOutput rows indicates we've already done the assembly, so we skip
        long existingOutputs = jobOutputRepository.countByJob(job);
        if (existingOutputs > 0) {
            log.info("assembly for job {}: output already delivered ({} JobOutput row(s)) - skipping",
                    jobUuid, existingOutputs);
            return;
        }

        List<PrototypeBatchMetadataRepository.CompletedPartition> partitions =
                batchMeta.completedPartitionFiles(jobUuid, PrototypeJobProcessorImpl.WORKER_STEP_NAME);

        if (partitions.isEmpty()) {
            log.info("assembly for job {}: no completed partitions - empty result, no output files", jobUuid);
            return;
        }

        crashInjector.maybeCrash("assemble");

        Path streamingDir = searchConfig.getStreamingDir(jobUuid).toPath();
        Path finishedDir = searchConfig.getFinishedDir(jobUuid).toPath();
        Path jobRoot = Path.of(searchConfig.getEfsMount(), jobUuid);
        long rolloverBytes = (long) searchConfig.getNdjsonRollOver() * BYTES_PER_MB;

        Files.createDirectories(finishedDir);
        Files.createDirectories(jobRoot);

        // Data stream is required: a missing data winner means the delivered output would be incomplete.
        List<Path> dataWinners = promoteWinners(partitions, contractNumber, PrototypePartitionNaming.DATA_STREAM,
                streamingDir, finishedDir, jobUuid, true);
        List<Path> dataRollover = concatenate(dataWinners, contractNumber, jobRoot, rolloverBytes,
                PrototypePartitionNaming.DATA_STREAM);

        // Error stream is optional: a partition with no serialization failures has an empty error file.
        List<Path> errorWinners = promoteWinners(partitions, contractNumber, PrototypePartitionNaming.ERROR_STREAM,
                streamingDir, finishedDir, jobUuid, false);
        List<Path> errorRollover = concatenate(errorWinners, contractNumber, jobRoot, rolloverBytes,
                PrototypePartitionNaming.ERROR_STREAM);

        List<JobOutput> outputs = new ArrayList<>(dataRollover.size() + errorRollover.size());
        registerOutputs(dataRollover, job, outputs);
        registerOutputs(errorRollover, job, outputs);
        if (!outputs.isEmpty()) {
            jobOutputRepository.saveAll(outputs);
        }
        log.info("assembly for job {}: {} data + {} error rollover file(s) from {} winning partition(s), "
                + "{} new JobOutput row(s)",
                jobUuid, dataRollover.size(), errorRollover.size(), dataWinners.size(), outputs.size());
    }

    /**
     * Compress each rollover file and register a JobOutput row for it. Idempotency is handled by parent.
     */
    private void registerOutputs(List<Path> rolloverFiles, Job job, List<JobOutput> outputs)
            throws IOException {
        for (Path file : rolloverFiles) {
            StreamOutput streamOutput = new StreamOutput(file.toFile());
            JobOutput output = new JobOutput();
            output.setFilePath(streamOutput.getFilePath());
            output.setFhirResourceType(BundleUtils.EOB);
            output.setError(streamOutput.getType() == FileOutputType.ERROR
                    || streamOutput.getType() == FileOutputType.ERROR_COMPRESSED);
            output.setChecksum(streamOutput.getChecksum());
            output.setFileLength(streamOutput.getFileLength());
            job.addJobOutput(output);
            outputs.add(output);
        }
    }

    /**
     * Remove the streaming/ and finished/ directories if the job is successful
     */
    public void deleteIntermediateDirectories(String jobUuid) {
        boolean streaming = FileSystemUtils.deleteRecursively(searchConfig.getStreamingDir(jobUuid));
        boolean finished = FileSystemUtils.deleteRecursively(searchConfig.getFinishedDir(jobUuid));
        log.info("cleanup for job {}: removed intermediate directories (streaming={}, finished={})",
                jobUuid, streaming, finished);
    }

    /**
     * Remove the entire job output directory. Called on a terminal failure or cancellation so we do not
     * leave partial output lying around on EFS.
     */
    public void deleteJobDirectory(String jobUuid) {
        boolean removed = FileSystemUtils.deleteRecursively(Path.of(searchConfig.getEfsMount(), jobUuid).toFile());
        log.info("cleanup for job {}: removed job output directory (existed={})", jobUuid, removed);
    }

    /**
     * Takes as an argument a list of the winning partition files (already determined earlier) and
     * promotes them, checking the finished directory to see if there is already a winner there from
     * a prior assembly that got interrupted.
     *
     * @param required if true a missing winner is fatal. Missing winners for data files is fatal
     *                      missing winners for error files is fine, there might not be one
     */
    private List<Path> promoteWinners(List<PrototypeBatchMetadataRepository.CompletedPartition> partitions,
                                      String contractNumber, String suffix, Path streamingDir, Path finishedDir,
                                      String jobUuid, boolean required) throws IOException {
        List<Path> promoted = new ArrayList<>(partitions.size());
        for (PrototypeBatchMetadataRepository.CompletedPartition partition : partitions) {
            String name = PrototypePartitionNaming.fileName(contractNumber, partition.partitionIndex(),
                    partition.token(), suffix);
            Path source = streamingDir.resolve(name);
            Path dest = finishedDir.resolve(name);
            if (Files.isRegularFile(source)) {
                if (!required && Files.size(source) == 0) {
                    continue;   // empty file for this partition, but it isn't required
                }
                Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
                promoted.add(dest);
            } else if (Files.isRegularFile(dest)) {
                if (!required && Files.size(dest) == 0) {
                    continue;
                }
                promoted.add(dest);   // already promoted by an earlier, interrupted assembly
            } else if (required) {
                throw new IOException("assembly for job " + jobUuid + ": winning file missing for partition "
                        + partition.partitionIndex() + " (token " + partition.token() + "): " + source
                        + " - the output is incomplete, so the assembly is terminated");
            }
        }
        return promoted;
    }

    /**
     * Concatenates the winning files into ndjson of the desired size. Returns an empty list when there are
     * no winners (like in the case of an error stream with no errors).
     */
    private List<Path> concatenate(List<Path> winners, String contractNumber, Path jobRoot, long rolloverBytes,
                                   String suffix) throws IOException {
        List<Path> rolloverFiles = new ArrayList<>();
        int sequence = 0;
        OutputStream current = null;
        long currentSize = 0;
        try {
            for (Path winner : winners) {
                long sourceSize = Files.size(winner);
                if (current != null && currentSize > 0 && currentSize + sourceSize > rolloverBytes) {
                    current.close();
                    current = null;
                }
                if (current == null) {
                    Path currentPath = jobRoot.resolve(rolloverFileName(contractNumber, ++sequence, suffix));
                    current = Files.newOutputStream(currentPath);
                    currentSize = 0;
                    rolloverFiles.add(currentPath);
                }
                Files.copy(winner, current);
                currentSize += sourceSize;
            }
        } finally {
            if (current != null) {
                current.close();
            }
        }
        return rolloverFiles;
    }

    private static String rolloverFileName(String contractNumber, int sequence, String suffix) {
        return contractNumber + "_" + String.format("%04d", sequence) + suffix + ".ndjson";
    }
}
