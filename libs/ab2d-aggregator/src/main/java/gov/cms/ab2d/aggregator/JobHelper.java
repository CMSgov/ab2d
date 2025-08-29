package gov.cms.ab2d.aggregator;

import java.io.File;
import java.io.IOException;

import static gov.cms.ab2d.aggregator.FileUtils.createADir;
import static gov.cms.ab2d.aggregator.FileUtils.deleteAllInDir;

/**
 * This class facilitates communication between the "worker" and the "aggregator". Currently, this is done
 * by the presence of different directories. When a job is created using workerSetUpJobDirectories, it creates
 * three directories. One for the job, one for any open streaming files (under the job directory) and one for
 * any finished streaming files (also under the job directory). To aggregate files, the aggregator looks in
 * the finished directory and combines them as necessary and puts the final files into the top level job directory.
 * When the worker is done streaming files, it deletes the streaming directory. This lets the aggregator it can go ahead
 * and combine any remaining files in the finished directory. When the aggregator is done aggregating files (the finished
 * directory is empty), it deletes the finished directory. At this point, the resulting files in the top level job
 * directory will look exactly as it did before the aggregator was created, a list of sequentially numbered files in
 * the job directory close to the max size of files. The worker will know that the aggregator is done when the finished
 * directory is gone. The Callable handles this and the thread is "done" when that happens.
 *
 * The aggregator knows that there are still files to be streamed if the open streaming directory still exists.
 * The worker knows that there are still files to be aggregated if the finished streaming directory still exists.
 * When the aggregator is done, all the files will be aggregated into the main job directory
 */
public final class JobHelper {
    private JobHelper() { }

    /**
     * This does the work of creating all the necessary directories for the job
     * @param jobId - the job id and the root directory
     * @throws IOException if there is a problem creating the directories
     */
    public static void workerSetUpJobDirectories(String jobId, String baseDir, String streamDir, String finishedDir) throws IOException {
        // Create job directory
        createADir(baseDir + File.separator + jobId);

        // Create a directory that we're going to dump all the streaming files
        createADir(baseDir + File.separator + jobId + File.separator + streamDir);

        // Create the directory where we're going to put all the finished streams
        createADir(baseDir + File.separator + jobId + File.separator + finishedDir);
    }


    /**
     * This allows the worker to send a message to the aggregator that it is done streaming data. This
     * deletes the streaming directory
     *
     * @param streamingDir - the location where all finished files are put by the worker
     */
    public static void workerFinishJob(String streamingDir) {
        deleteAllInDir(streamingDir);
    }

    /**
     * This allows the aggregator to let the job know that the aggregator has finished aggregating EOB files
     *
     * @param finishedDir - the job id
     */
    public static void aggregatorFinishJob(String finishedDir) {
        deleteAllInDir(finishedDir);
    }
}
