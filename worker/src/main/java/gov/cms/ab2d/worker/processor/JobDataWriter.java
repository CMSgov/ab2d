package gov.cms.ab2d.worker.processor;

import java.nio.file.Path;
import java.util.List;

/**
 *
 */
public interface JobDataWriter {

    /**
     * Writes a chunk of data to the output file. If needed, transparently rolls the file over to the next one,
     * e.g. S00001_0001.ndjson, S00001_0002.ndjson, and so on.
     * This method MUST be thread-safe and do appropriate locking, e.g. using ReentrantLock
     * @param data
     */
    void addDataEntry(byte[] data);

    /**
     * Writes a chunk of data to the error file. Roll-over not supported at this point but MAY
     * BE required in future if we determine errors files are getting too big.
     * This method MUST be thread-safe and do appropriate locking, e.g. using ReentrantLock
     * @param data
     */
    void addErrorEntry(byte[] data);


    /**
     * Once the job is all done, we call this method to find out what output files have been created.
     */
    List<Path> getDataFiles();

    /**
     * Once the job is all done, we call this method to find out what error files have been created, if any.
     */
    List<Path> getErrorFiles();

}
