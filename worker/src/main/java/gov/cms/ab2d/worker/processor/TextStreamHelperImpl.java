package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.util.EventUtils;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Path;

/**
 * Implement a plain text stream helper
 */
@Slf4j
public class TextStreamHelperImpl extends StreamHelperImpl {

    private File currentFile;

    private static final int MIB = 1048576;

    /**
     * Implement the text stream helper
     *
     * @param path - where to create the file
     * @param contractNumber - the contract number
     * @param totalBytesAllowed - the total number of bytes allowed to be written to the stream
     * @param tryLockTimeout - the amount of time to wait before timing out lock
     * @throws FileNotFoundException - if the file can't be created
     */
    public TextStreamHelperImpl(Path path, String contractNumber, long totalBytesAllowed, int tryLockTimeout,
                                LogManager logger, Job job)
            throws FileNotFoundException {
        super(path, contractNumber, totalBytesAllowed, tryLockTimeout, logger, job);

        currentStream = createStream();
    }

    /**
     * Create the next stream from the next file name
     *
     * @return the stream
     * @throws FileNotFoundException if you can't create the stream
     */
    private OutputStream createStream() throws FileNotFoundException {
        String fileName = path.toString() + File.separator + createFileName();
        File f = new File(fileName);
        f.getParentFile().mkdirs();
        currentFile = f;
        logManager.log(EventUtils.getFileEvent(job, f, FileEvent.FileStatus.OPEN));
        OutputStream stream = new BufferedOutputStream(new FileOutputStream(fileName), MIB);
        Path p = Path.of(fileName);
        filesCreated.add(p);
        return stream;
    }

    /**
     * Write data to the correct file and iterate to the next file if it exceeds the limit
     *
     * @param data - the data to write
     */
    @Trace
    @Override
    public void addData(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return;
        }

        tryLock(dataFileLock);
        try {
            if (exceedsMaxFileSize(data)) {
                currentStream.close();
                logManager.log(EventUtils.getFileEvent(job, currentFile, FileEvent.FileStatus.CLOSE));

                createStreamOutput(currentFile, false);

                currentStream = createStream();
                setTotalBytesWritten(0);

            }
            currentStream.write(data);
            setTotalBytesWritten(getTotalBytesWritten() + data.length);

        } catch (Exception ex) {
            String error = "Unable to create file output stream for contract " + contractNumber + "[" + (counter - 1) + "]";
            log.error(error, ex);
            throw new IOException(error, ex);
        } finally {
            dataFileLock.unlock();
        }
    }

    private boolean exceedsMaxFileSize(byte[] data) {
        return getTotalBytesWritten() + data.length > getTotalBytesAllowed() && getTotalBytesWritten() > 0;
    }

    @Override
    public void closeLastStream() throws IOException {

        if (currentStream == null) {
            return;
        }

        currentStream.close();
        logManager.log(EventUtils.getFileEvent(job, currentFile, FileEvent.FileStatus.CLOSE));
        int numFiles = filesCreated.size();
        if (filesCreated.get(numFiles - 1).toFile().length() == 0) {
            filesCreated.remove(numFiles - 1);
        } else {
            createStreamOutput(currentFile, false);
        }

        // Current stream should never be used again
        currentStream = null;
    }

    /**
     * Close the stream clean up any empty files in the files created list
     */
    @Override
    public void close() throws IOException {
        try {
            closeLastStream();
        } catch (Exception ex) {
            String error = "Unable to close output stream for contract " + contractNumber + "[" + counter + "]";
            log.error(error, ex);
            throw new IOException(error, ex);
        }
    }
}
