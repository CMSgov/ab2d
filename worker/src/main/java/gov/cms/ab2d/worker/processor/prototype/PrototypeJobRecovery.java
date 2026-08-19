package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.worker.config.SearchConfig;
import gov.cms.ab2d.worker.processor.prototype.lease.JobLeaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

import static gov.cms.ab2d.worker.processor.prototype.PrototypeJobProcessorImpl.JOB_UUID_PARAM;
import static gov.cms.ab2d.worker.processor.prototype.PrototypeJobProcessorImpl.PROTOTYPE_JOB_NAME;
import static gov.cms.ab2d.worker.processor.prototype.PrototypeJobProcessorImpl.WORKER_STEP_NAME;
import static gov.cms.ab2d.worker.processor.prototype.PrototypePartitionNaming.CURRENT_COUNT_SUFFIX;
import static gov.cms.ab2d.worker.processor.prototype.PrototypePartitionNaming.DATA_STREAM;
import static gov.cms.ab2d.worker.processor.prototype.PrototypePartitionNaming.ERROR_STREAM;
import static gov.cms.ab2d.worker.processor.prototype.PrototypePartitionNaming.WRITTEN_SUFFIX;

/**
 * Everything a worker does to take over a job before it can run it.
 * <p>
 * Handles both the soft resume, which just adopts the current token and picks up the work where it left
 * off, and hard recovery, which bumps the token, updates the checkpoints, and can copy old work into the new
 * file.
 * </p>
 */
@Slf4j
@Component
public class PrototypeJobRecovery {

    private final JobLeaseRepository jobLease;
    private final JobRepository batchJobRepository;
    private final PrototypeBatchMetadataRepository batchMeta;
    private final SearchConfig searchConfig;
    private final PrototypeProperties props;

    public PrototypeJobRecovery(JobLeaseRepository jobLease, JobRepository batchJobRepository,
                                PrototypeBatchMetadataRepository batchMeta, SearchConfig searchConfig,
                                PrototypeProperties props) {
        this.jobLease = jobLease;
        this.batchJobRepository = batchJobRepository;
        this.batchMeta = batchMeta;
        this.searchConfig = searchConfig;
        this.props = props;
    }

    /** Record of whether this worker soft or hard restarted this job, and its token  */
    public record Ownership(long fenceToken, boolean softResume) {
    }

    /**
     * Claim a job and leave its Spring Batch state in a shape that can be restarted.
     */
    public Ownership acquire(String jobUuid, String owner) {
        Optional<Long> adopted = jobLease.tryAdoptCleanSuspend(jobUuid, owner);
        if (adopted.isPresent()) {
            // The token did not move, so the files and checkpoint keys already line up. Nothing to repair.
            return new Ownership(adopted.get(), true);
        }

        long fenceToken = jobLease.bump(jobUuid, owner);
        // Copy-forward runs first, to see if there are any UNKNOWN statuses
        // If no, it copies the old partition work to the new file
        // If yes, it doesn't copy anything forward, the partition will be restarted
        copyForward(jobUuid, fenceToken);
        batchMeta.healIndeterminateExecutions(jobUuid);
        return new Ownership(fenceToken, false);
    }

    /**
     * If the partition is not in status UNKNOWN, and hasn't been completed, copy its work
     * over to the new file
     */
    private int copyForward(String jobUuid, long newToken) {
        if (!props.isCopyForwardEnabled()) {
            log.info("copy-forward is disabled for job {}", jobUuid);
            return 0;
        }

        JobInstance instance = batchJobRepository.getJobInstance(PROTOTYPE_JOB_NAME,
                new JobParametersBuilder().addString(JOB_UUID_PARAM, jobUuid).toJobParameters());
        if (instance == null) {
            return 0;
        }

        int copied = 0;
        for (String stepName : batchMeta.partitionStepNames(jobUuid, WORKER_STEP_NAME)) {
            if (copyForwardPartition(jobUuid, instance, stepName, newToken)) {
                copied++;
            }
        }
        if (copied > 0) {
            log.info("copy-forward for job {}: seeded {} partition(s) into token {}", jobUuid, copied, newToken);
        }
        return copied;
    }

    /**
     * Copy forward a single partition. If the partition cannot successfully be
     * copied forward, i.e. it has an unknown step execution status or is already
     * completed, the executionContext is left untouched and the partition will
     * behave "normally". Normal in this case means restarting the partition from scratch.
     *
     * @return true if the partition's checkpoint was updated
     */
    private boolean copyForwardPartition(String jobUuid, JobInstance instance, String stepName, long newToken) {
        StepExecution last = batchJobRepository.getLastStepExecution(instance, stepName);
        if (last == null || last.getStatus() == BatchStatus.COMPLETED) {
            return false;
        }
        if (last.getStatus() == BatchStatus.UNKNOWN) {
            log.info("copy-forward for job {}: {} ended UNKNOWN, so its partition " +
                    "will be restarted", jobUuid, stepName);
            return false;
        }

        ExecutionContext context = last.getExecutionContext();
        String contractNumber = context.getString(BeneficiaryPartitioner.KEY_CONTRACT, null);
        int partitionIndex = context.getInt(BeneficiaryPartitioner.KEY_PARTITION_INDEX, -1);
        if (contractNumber == null || partitionIndex < 0) {
            log.warn("copy-forward for job {}: {} is missing the contract number or partition index the " +
                            "partitioner writes, so the partition must be restarted",
                    jobUuid, stepName);
            return false;
        }

        Checkpoint checkpoint = readCheckpoint(context, partitionIndex);
        if (checkpoint == null) {
            log.info("copy-forward for job {}: {} has no committed chunks, so it " +
                    "will be restarted", jobUuid, stepName);
            return false;
        }

        Path streamingDir = searchConfig.getStreamingDir(jobUuid).toPath();
        List<Path> seeded = new ArrayList<>(2);
        try {
            Files.createDirectories(streamingDir);
            seeded.add(seedFile(streamingDir, contractNumber, checkpoint, newToken, DATA_STREAM,
                    checkpoint.dataBytes()));
            seeded.add(seedFile(streamingDir, contractNumber, checkpoint, newToken, ERROR_STREAM,
                    checkpoint.errorBytes()));
        } catch (IOException e) {
            deleteQuietly(seeded);
            log.warn("copy-forward for job {}: could not seed {} from token {}, so the partition must restart",
                    jobUuid, stepName, checkpoint.token(), e);
            return false;
        }

        retarget(context, checkpoint, newToken);
        batchJobRepository.updateExecutionContext(last);
        log.info("copy-forward for job {}: {} resumes at patient {} with {} data byte(s) and {} error byte(s) "
                        + "carried from token {} into token {}",
                jobUuid, stepName, checkpoint.readerCursor(), checkpoint.dataBytes(), checkpoint.errorBytes(),
                checkpoint.token(), newToken);
        return true;
    }

    /**
     * Gets a partition's cursor as of its last completed chunk.
     * We need both the reader and writer cursor from the prior execution, so we can
     * copy forward the correct data, and avoid rereading the part of the partition
     * we've already processed.
     */
    private Checkpoint readCheckpoint(ExecutionContext context, int partitionIndex) {
        long token = -1;
        for (String key : keysOf(context)) {
            Matcher matcher = PrototypePartitionNaming.DATA_WRITER_OFFSET_KEY.matcher(key);
            if (matcher.matches()) {
                token = Math.max(token, Long.parseLong(matcher.group(1)));
            }
        }
        if (token < 0) {
            return null;
        }

        String data = PrototypePartitionNaming.dataWriterName(partitionIndex, token);
        String error = PrototypePartitionNaming.errorWriterName(partitionIndex, token);
        String cursor = PrototypePartitionNaming.readerCursorKey(token);
        if (!context.containsKey(data + WRITTEN_SUFFIX)
                || !context.containsKey(error + CURRENT_COUNT_SUFFIX)
                || !context.containsKey(error + WRITTEN_SUFFIX)
                || !context.containsKey(cursor)) {
            return null;
        }
        return new Checkpoint(partitionIndex, token,
                context.getLong(data + CURRENT_COUNT_SUFFIX), context.getLong(data + WRITTEN_SUFFIX),
                context.getLong(error + CURRENT_COUNT_SUFFIX), context.getLong(error + WRITTEN_SUFFIX),
                context.getLong(cursor));
    }

    /**
     * Take the bytes that have been committed from the prior worker's work, and copy
     * them over to the file that the new worker will open.
     *
     * @return the file that was written, so a later failure can clean it up
     */
    private Path seedFile(Path streamingDir, String contractNumber, Checkpoint checkpoint, long newToken,
                          String streamSuffix, long bytes) throws IOException {
        Path source = streamingDir.resolve(PrototypePartitionNaming.fileName(
                contractNumber, checkpoint.partitionIndex(), checkpoint.token(), streamSuffix));
        Path destination = streamingDir.resolve(PrototypePartitionNaming.fileName(
                contractNumber, checkpoint.partitionIndex(), newToken, streamSuffix));

        if (bytes > 0) {
            if (!Files.isRegularFile(source)) {
                throw new IOException("missing source file " + source + " for " + bytes + " committed byte(s)");
            }
            long available = Files.size(source);
            if (available < bytes) {
                throw new IOException("source file " + source + " holds " + available + " byte(s), fewer than the "
                        + bytes + " recorded at the last chunk commit");
            }
        }

        try (FileChannel out = FileChannel.open(destination, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            copyPrefix(source, out, bytes);
            out.force(true);
        }
        return destination;
    }

    private void copyPrefix(Path source, FileChannel out, long bytes) throws IOException {
        if (bytes == 0) {
            return;
        }
        try (FileChannel in = FileChannel.open(source, StandardOpenOption.READ)) {
            long written = 0;
            while (written < bytes) {
                long transferred = out.transferFrom(in, written, bytes - written);
                if (transferred <= 0) {
                    throw new IOException("copy of " + source + " stalled after " + written + " of " + bytes
                            + " byte(s)");
                }
                written += transferred;
            }
        }
    }

    /**
     * Takes the old context, drops all its checkpoint keys, then adds them back under the new
     * token. Manually brings the execution context up to the new checkpoint.
     */
    private void retarget(ExecutionContext context, Checkpoint checkpoint, long newToken) {
        keysOf(context).stream()
                .filter(PrototypePartitionNaming::isCheckpointKey)
                .forEach(context::remove);

        String data = PrototypePartitionNaming.dataWriterName(checkpoint.partitionIndex(), newToken);
        String error = PrototypePartitionNaming.errorWriterName(checkpoint.partitionIndex(), newToken);
        context.putLong(data + CURRENT_COUNT_SUFFIX, checkpoint.dataBytes());
        context.putLong(data + WRITTEN_SUFFIX, checkpoint.dataLines());
        context.putLong(error + CURRENT_COUNT_SUFFIX, checkpoint.errorBytes());
        context.putLong(error + WRITTEN_SUFFIX, checkpoint.errorLines());
        context.putLong(PrototypePartitionNaming.readerCursorKey(newToken), checkpoint.readerCursor());
    }

    /** A snapshot of the context's keys, safe to iterate while removing entries. */
    private static List<String> keysOf(ExecutionContext context) {
        return context.entrySet().stream().map(Map.Entry::getKey).toList();
    }

    private void deleteQuietly(List<Path> files) {
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                log.warn("copy-forward could not remove the partially seeded file {}", file, e);
            }
        }
    }

    /** A record of a partition's last checkpoint, including the token it belonged to */
    private record Checkpoint(int partitionIndex, long token, long dataBytes, long dataLines,
                              long errorBytes, long errorLines, long readerCursor) {
    }
}
