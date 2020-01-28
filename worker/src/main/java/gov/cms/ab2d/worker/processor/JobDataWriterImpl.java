package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Contract;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Since the Worker processes contracts sequentially, create a new instance for each contract,
 * and make sure only that single instance is shared among the threads.
 */
public class JobDataWriterImpl implements JobDataWriter {

    private Lock lock = new ReentrantLock();


    public JobDataWriterImpl(Path outputDir, Contract contract, long fileSizeRollOverThresholdInMegabytes) {
    }


    /**
     * Writes a chunk of data to the output file. If needed, transparently rolls the file over to
     * the next one,
     * e.g. S00001_0001.ndjson, S00001_0002.ndjson, and so on.
     * This method MUST be thread-safe and do appropriate locking, e.g. using ReentrantLock
     *
     * @param data
     */
    @Override
    public void addDataEntry(byte[] data) {

    }

    /**
     * Writes a chunk of data to the error file. Roll-over not supported at this point but MAY
     * BE required in future if we determine errors files are getting too big.
     * This method MUST be thread-safe and do appropriate locking, e.g. using ReentrantLock
     *
     * @param data
     */
    @Override
    public void addErrorEntry(byte[] data) {

    }

    /**
     * Once the job is all done, call this method to tell the Writer that no more output is
     * expected so that
     * files could be closed, buffers flushed as needed, etc.
     */
    @Override
    public void close() {

    }

    /**
     * Once the job is all done, we call this method to find out what output files have been
     * created.
     */
    @Override
    public List<Path> getDataFiles() {
        return null;
    }

    /**
     * Once the job is all done, we call this method to find out what error files have been
     * created, if any.
     */
    @Override
    public List<Path> getErrorFiles() {
        return null;
    }
}
