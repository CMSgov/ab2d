package gov.cms.ab2d.worker.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;

@Component
@Data
/**
 * Loads all the configuration components of a BFD search, data writing and aggregation
 */
public class SearchConfig {
    /**
     * The location where job directories are created
     */
    private final String efsMount;
    /**
     * The name of the directory for the worker to stream data to
     */
    private final String streamingDir;
    /**
     * The name of the directory for the worker place files that it has finished streaming
     */
    private final String finishedDir;
    /**
     * The write buffer size of the temporary files that contain the pre-aggregated
     * temporary files
     */
    private final int bufferSize;
    /**
     * The max size we want the resulting aggregated file to be
     */
    private final int ndjsonRollOver;
    /**
     * The multiplier of ndjsonRollOver to let the aggregator know how large the sum of the
     * sizes of all files in the "finished" directory should be before its starts aggregating files.
     * For example, if the multiplier is 3 and the ndjsonRollover value is 200, the aggregator
     * won't start aggregating until the total size of the files in the "finished" directory
     * is over 600 MB and will stop aggregating when it dips below that value until
     * it surpasses it again or the job has finished writing data
     */
    private final int multiplier;

    /**
     * Each search thread takes a number of beneficiaries, search for all those beneficiaries
     * in BFD and then writes out the resulting EOBs to a temporary file (to later be aggregated
     * into the final files). This value indicates the number of beneficiaries each search thread
     * should do.
     */
    private final int numberBenesPerBatch;

    public SearchConfig(@Value("${efs.mount}") String efsMount,
                        @Value("${aggregator.directory.streaming}") String streamingDir,
                        @Value("${aggregator.directory.finished}") String finishedDir,
                        @Value("${aggregator.file.buffer.size}") int bufferSize,
                        @Value("${job.file.rollover.ndjson:200}") int ndjsonRollover,
                        @Value("${aggregator.multiplier}") int multiplier,
                        @Value("${eob.job.patient.number.per.thread}") int numberBenesPerBatch) {
        this.efsMount = efsMount;
        this.streamingDir = streamingDir;
        this.finishedDir = finishedDir;
        this.bufferSize = bufferSize;
        this.ndjsonRollOver = ndjsonRollover;
        this.multiplier = multiplier;
        this.numberBenesPerBatch = numberBenesPerBatch;
    }

    public File getStreamingDir(String jobId) {
        return Path.of(efsMount, jobId, streamingDir).toFile();
    }

    public File getFinishedDir(String jobId) {
        return Path.of(efsMount, jobId, finishedDir).toFile();
    }
}
