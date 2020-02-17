package gov.cms.ab2d.worker.processor;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Path;

/**
 * Implement a plain text stream helper
 */
@Slf4j
public class TextStreamHelperImpl extends StreamHelperImpl {

    /**
     * Implement the text stream helper
     *
     * @param path - where to create the file
     * @param contractNumber - the contract number
     * @param totalBytesAllowed - the total number of bytes allowed to be written to the stream
     * @throws FileNotFoundException - if the file can't be created
     */
    TextStreamHelperImpl(Path path, String contractNumber, long totalBytesAllowed, int tryLockTimeout)
            throws FileNotFoundException {
        super(path, contractNumber, totalBytesAllowed, tryLockTimeout);

        setCurrentStream(createStream());
    }

    /**
     * Create the next stream from the next file name
     *
     * @return the stream
     * @throws FileNotFoundException if you can't create the stream
     */
    private OutputStream createStream() throws FileNotFoundException {
        String fileName = getPath().toString() + "/" + createFileName();
        File f = new File(fileName);
        f.getParentFile().mkdirs();
        OutputStream stream = new BufferedOutputStream(new FileOutputStream(fileName));
        Path p = Path.of(fileName);
        getFilesCreated().add(p);
        return stream;
    }

    /**
     * Write data to the correct file and iterate to the next file if it exceeds the limit
     *
     * @param data - the data to write
     */
    @Override
    public void addData(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return;
        }
        tryLock(getDataFileLock());
        try {
            if (getCurrentStream() == null) {
                setCurrentStream(createStream());
                setTotalBytesWritten(0);
            }
            if (getTotalBytesWritten() + data.length > getTotalBytesAllowed() && getTotalBytesWritten() > 0) {
                getCurrentStream().close();
                setCurrentStream(createStream());
                setTotalBytesWritten(0);
            }
            getCurrentStream().write(data);
            setTotalBytesWritten(getTotalBytesWritten() + data.length);
        } catch (Exception ex) {
            log.error("Unable to create file output stream for contract " + getContractNumber() + "[" + (getCounter() - 1) + "]", ex);
            throw ex;
        } finally {
            getDataFileLock().unlock();
        }
    }

    /**
     * Close the stream clean up any empty files in the files created list
     */
    @Override
    public void close() {
        if (getCurrentStream() == null) {
            return;
        }
        try {
            getCurrentStream().close();
            int numFiles = getFilesCreated().size();
            if (getFilesCreated().get(numFiles - 1).toFile().length() == 0) {
                getFilesCreated().remove(numFiles - 1);
            }
        } catch (Exception ex) {
            log.error("Unable to close output stream for contract " + getContractNumber() + "[" + getCounter() + "]", ex);
        }
    }
}
