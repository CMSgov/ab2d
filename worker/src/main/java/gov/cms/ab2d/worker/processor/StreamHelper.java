package gov.cms.ab2d.worker.processor;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Basic interface for managing a stream writing out EOBs to files
 */
public interface StreamHelper extends Closeable {

    /**
     * Writes byte array out to a file, will write all the data to a single file and not split the data between files
     * @param data data to write to the file
     * @return a stream output if the maximum file size has been reached and a file has been closed
     * @throws IOException on failure to write data to a file
     */
    Optional<StreamOutput> addData(byte[] data) throws IOException;

    /**
     * Write out error to the error file. There should be a single error file
     * @param data data to write to the file
     * @throws IOException on failure to write data to the error file
     */
    void addError(String data) throws IOException;

    /**
     * Handle the last non-empty file. Any additional usage of the stream will throw an error
     * @return a stream output if a non-empty file is open and was closed
     * @throws IOException on failure to write data to a file
     */
    Optional<StreamOutput> closeLastStream() throws IOException;

    List<Path> getDataFiles();
    List<Path> getErrorFiles();
    void close() throws IOException;
}
