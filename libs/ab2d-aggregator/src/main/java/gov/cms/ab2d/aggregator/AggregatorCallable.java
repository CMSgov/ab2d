package gov.cms.ab2d.aggregator;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import static gov.cms.ab2d.aggregator.Aggregator.AggregatorResult.PERFORMED;
import static gov.cms.ab2d.aggregator.FileOutputType.DATA;
import static gov.cms.ab2d.aggregator.FileOutputType.ERROR;

/**
 * This Callable allows us to hover over the file directory until all data is streamed out
 * by the worker and occasionally aggregate
 */
@Getter
@Slf4j
public class AggregatorCallable implements Callable<Integer> {
    private final String jobId;
    private final String contractId;
    private final String baseDir;
    private final String streamDir;
    private final String finishedDir;
    private final int maxMegaBytes;
    private final int multiplier;

    public AggregatorCallable(String baseDir, String jobId, String contractId, int maxMegaBytes, String streamDir,
                              String finishedDir, int multiplier) {
        this.jobId = jobId;
        this.contractId = contractId;
        this.baseDir = baseDir;
        this.maxMegaBytes = maxMegaBytes;
        this.streamDir = streamDir;
        this.finishedDir = finishedDir;
        this.multiplier = multiplier;
    }

    @Override
    public Integer call() throws Exception {
        int numAggregations = 0;
        // Create a new aggregator for the job
        Aggregator aggregator = new Aggregator(jobId, contractId, baseDir, maxMegaBytes, streamDir, finishedDir, multiplier);
        // While the worker isn't done with streaming files
        while (!aggregator.isJobDoneStreamingData()) {
            // aggregate data files
            try {
                while (aggregator.aggregate(DATA) == PERFORMED) {
                    numAggregations++;
                }
                // aggregate error files
                while (aggregator.aggregate(ERROR) == PERFORMED) {
                    numAggregations++;
                }
            } catch (IOException io) {
                log.error("There was an error while trying to aggregate files", io);
            }
            // Sleep a little between checks
            Thread.sleep(1000);
        }
        // aggregate the final data files
        try {
            while (aggregator.aggregate(DATA) == PERFORMED) {
                numAggregations++;
            }
            // aggregate the final error files
            while (aggregator.aggregate(ERROR) == PERFORMED) {
                numAggregations++;
            }
        } catch (IOException ex) {
            log.error("There was an error aggregating the final files of the job", ex);
        }

        // We've taken all the files that the worker has given us, "finish" the job so that
        // the worker knows we're done
        JobHelper.aggregatorFinishJob(this.baseDir + File.separator + this.jobId + File.separator + this.finishedDir);
        return numAggregations;
    }
}
