package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobOutput;
import gov.cms.ab2d.job.repository.JobOutputRepository;
import gov.cms.ab2d.worker.config.SearchConfig;
import gov.cms.ab2d.worker.processor.StreamOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
 * TODO: Prototype simplifications that will need to be fixed on integration:
 *      Streaming/finished directories need to be deleted
 *      Output needs to be compressed
 *      No error ndjson produced
 */
@Slf4j
@Component
public class PrototypeOutputAssembler {

    private static final int BYTES_PER_MB = 1024 * 1024;

    private final SearchConfig searchConfig;
    private final JobOutputRepository jobOutputRepository;
    private final PrototypeBatchMetadataRepository batchMeta;

    public PrototypeOutputAssembler(SearchConfig searchConfig, JobOutputRepository jobOutputRepository,
                                    PrototypeBatchMetadataRepository batchMeta) {
        this.searchConfig = searchConfig;
        this.jobOutputRepository = jobOutputRepository;
        this.batchMeta = batchMeta;
    }

    /**
     * Promotes winning partition files and then concatenates the winners into finished 200mb files
     * Creates a JobOutput per finished file.
     *
     * @throws IOException If a winning file is missing, the job is a failure and cannot be completed.
     */
    public void assemble(Job job, String jobUuid, String contractNumber) throws IOException {
        List<PrototypeBatchMetadataRepository.CompletedPartition> partitions =
                batchMeta.completedPartitionFiles(jobUuid, PrototypeJobProcessorImpl.WORKER_STEP_NAME);

        if (partitions.isEmpty()) {
            log.info("assembly for job {}: no completed partitions - empty result, no output files", jobUuid);
            return;
        }

        Path streamingDir = searchConfig.getStreamingDir(jobUuid).toPath();
        Path finishedDir = searchConfig.getFinishedDir(jobUuid).toPath();
        Path jobRoot = Path.of(searchConfig.getEfsMount(), jobUuid);
        long rolloverBytes = (long) searchConfig.getNdjsonRollOver() * BYTES_PER_MB;

        // Promote the winning component files streaming/ -> finished/, so finished/ ends up holding exactly the one
        // file per partition
        Files.createDirectories(finishedDir);
        List<Path> winners = promoteWinners(partitions, contractNumber, streamingDir, finishedDir, jobUuid);

        Files.createDirectories(jobRoot);
        List<Path> rolloverFiles = concatenate(winners, contractNumber, jobRoot, rolloverBytes);

        List<JobOutput> outputs = new ArrayList<>(rolloverFiles.size());
        for (Path file : rolloverFiles) {
            String fileName = file.getFileName().toString();
            // a worker that crashed between the batch being completed and the job being recorded successful
            // re-runs assembly on recovery. We skip files with existing JobOutput to avoid duplicates.
            if (jobOutputRepository.findByFilePathAndJob(fileName, job).isPresent()) {
                log.info("assembly for job {}: JobOutput for {} already registered - skipping (idempotent re-assembly)",
                        jobUuid, fileName);
                continue;
            }
            JobOutput output = new JobOutput();
            output.setFilePath(fileName);
            output.setFhirResourceType(BundleUtils.EOB);
            output.setError(false);
            output.setChecksum(StreamOutput.generateChecksum(file.toFile()));
            output.setFileLength(Files.size(file));
            job.addJobOutput(output);
            outputs.add(output);
        }
        if (!outputs.isEmpty()) {
            jobOutputRepository.saveAll(outputs);
        }
        log.info("assembly for job {}: promoted {} winning partition file(s) to finished/ and registered {} new "
                + "rollover manifest row(s) of {} rollover file(s)",
                jobUuid, winners.size(), outputs.size(), rolloverFiles.size());
    }

    /**
     * Promotes files from streaming to finished, and tolerates a winner already in finished from prior
     * crashed workers. File selection is deterministic, it's always the file with the highest value token.
     */
    private List<Path> promoteWinners(List<PrototypeBatchMetadataRepository.CompletedPartition> partitions,
                                      String contractNumber, Path streamingDir, Path finishedDir, String jobUuid)
            throws IOException {
        List<Path> promoted = new ArrayList<>(partitions.size());
        for (PrototypeBatchMetadataRepository.CompletedPartition partition : partitions) {
            String name = contractNumber + "_partition" + partition.partitionIndex()
                    + "_t" + partition.token() + ".ndjson";
            Path source = streamingDir.resolve(name);
            Path dest = finishedDir.resolve(name);
            if (!Files.isRegularFile(source)) {
                if (Files.isRegularFile(dest)) {
                    promoted.add(dest);   // already promoted by an earlier, interrupted assembly
                    continue;
                }
                throw new IOException("assembly for job " + jobUuid + ": winning file missing for partition "
                        + partition.partitionIndex() + " (token " + partition.token() + "): " + source
                        + " - refusing to deliver incomplete output");
            }
            Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
            promoted.add(dest);
        }
        return promoted;
    }

    /**
     * Concatenates the winning files into ndjson of the desired size.
     */
    private List<Path> concatenate(List<Path> winners, String contractNumber, Path jobRoot, long rolloverBytes)
            throws IOException {
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
                    Path currentPath = jobRoot.resolve(rolloverFileName(contractNumber, ++sequence));
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

    private static String rolloverFileName(String contractNumber, int sequence) {
        return contractNumber + "_" + String.format("%04d", sequence) + ".ndjson";
    }
}
