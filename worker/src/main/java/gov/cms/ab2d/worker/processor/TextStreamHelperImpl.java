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

        super.currentStream = createStream();
    }

    /**
     * Create the next stream from the next file name
     *
     * @return the stream
     * @throws FileNotFoundException if you can't create the stream
     */
    private OutputStream createStream() throws FileNotFoundException {
        String fileName = super.path.toString() + "/" + super.createFileName();
        File f = new File(fileName);
        f.getParentFile().mkdirs();
        OutputStream stream = new BufferedOutputStream(new FileOutputStream(fileName));
        Path p = Path.of(fileName);
        filesCreated.add(p);
        return stream;
    }

    /**
     * Write data to the correct file and iterate to the next file if it exceeds the limit
     *
     * @param data - the data to write
     */
    @Override
    public void addData(byte[] data) {
        tryLock(super.dataFileLock);
        if (data == null || data.length == 0) {
            return;
        }
        try {
            if (super.currentStream == null) {
                super.currentStream = createStream();
                super.totalBytesWritten = 0;
            }
            if (super.totalBytesWritten + data.length > super.totalBytesAllowed && totalBytesWritten > 0) {
                super.currentStream.close();
                super.currentStream = createStream();
                super.totalBytesWritten = 0;
            }
            super.currentStream.write(data);
            super.totalBytesWritten += data.length;
        } catch (Exception ex) {
            log.error("Unable to create file output stream for contract " + super.contractNumber + "[" + (super.counter - 1) + "]", ex);
        } finally {
            super.dataFileLock.unlock();
        }
    }

    /**
     * Close the stream clean up any empty files in the files created list
     */
    @Override
    public void close() {
        if (super.currentStream == null) {
            return;
        }
        try {
            super.currentStream.close();
            int numFiles = filesCreated.size();
            if (filesCreated.get(numFiles - 1).toFile().length() == 0) {
                filesCreated.remove(numFiles - 1);
            }
        } catch (Exception ex) {
            log.error("Unable to close output stream for contract " + contractNumber + "[" + counter + "]", ex);
        }
    }
}
