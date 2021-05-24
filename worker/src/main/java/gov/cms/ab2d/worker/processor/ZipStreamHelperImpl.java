package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.util.EventUtils;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Create a zip stream helper
 */
@Slf4j
public class ZipStreamHelperImpl extends StreamHelperImpl {
    private File currentFile;

    // Current index of zip file
    private int currentZipIteration = 1;

    // total bytes allowed for each expanded file part
    private long totalExpandedBytesInEntryAllowed;

    // The average amount of compression accross all files added (i.e., 0.2 means the resulting file is 1/5 the size
    // of the expanded file. Initialize it to no compression
    @Getter
    private double averageCompression = 1.0;

    // The byte stream for the current zip part
    private ByteArrayOutputStream currentPartByteStream;

    /**
     * Instantiate the super constructor but include two size limits; the size of the zip file and
     * the max uncompressed size a zip file entry can have before it gets added to the zip file
     *
     * @param path - where to put the output files
     * @param contractNumber - the contract number
     * @param totalBytesAllowed - the total bytes allowed to be written to the zip file (this is
     *                          approximate based on the average compression rate)
     * @param totalBytesInEntry - The total number of uncompressed bytes a zip entry must not
     *                          exceed before it is added to the zip file
     * @param tryLockTimeout - the lock time out on the files
     * @throws FileNotFoundException if there was an error writing to the file system
     */
    ZipStreamHelperImpl(Path path, String contractNumber, long totalBytesAllowed, long totalBytesInEntry,
                        int tryLockTimeout, LogManager logger, Job job) throws FileNotFoundException {
        super(path, contractNumber, totalBytesAllowed, tryLockTimeout, logger, job);
        this.totalExpandedBytesInEntryAllowed = totalBytesInEntry;
        checkInitStreams();
    }

    /**
     * Create a ZipOutputStream with the proper zip file name & iteration
     *
     * @return the stream
     * @throws FileNotFoundException if there was an error writing to the file system
     */
    private ZipOutputStream createStream() throws FileNotFoundException {
        String zipFileName = path.toString() + File.separator + createZipFileName();
        File f = new File(zipFileName);
        currentFile = f;
        logManager.log(EventUtils.getFileEvent(job, f, FileEvent.FileStatus.OPEN));
        f.getParentFile().mkdirs();
        Path currentFile = Path.of(zipFileName);
        FileOutputStream fos = new FileOutputStream(zipFileName);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        setTotalBytesWritten(0);
        filesCreated.add(currentFile);
        return new ZipOutputStream(bos);
    }

    /**
     * Returns the next zip file name
     *
     * @return the proper name
     */
    String createZipFileName() {
        var partName = Integer.toString(currentZipIteration);
        var paddedPartitionNo = StringUtils.leftPad(partName, 4, '0');
        currentZipIteration++;
        return contractNumber +
                "_" +
                paddedPartitionNo +
                FileOutputType.ZIP.getSuffix();
    }

    /**
     * Create a new zip file entry with the proper file name
     *
     * @return the zip file entry
     */
    private ZipEntry createFilePart() {
        return new ZipEntry(createFileName());
    }

    /**
     * Add data to the zip file. It determines if there are too many bytes for the zip file entry or
     * the zip file and either writes to the current entry, a new entry or a new zip file.
     *
     * @param data - the data to write
     */
    @Override
    public void addData(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return;
        }
        tryLock(dataFileLock);
        try {
            // If streams don't exist, create them
            checkInitStreams();
            // If the uncompressed file is larger than it should be, create new part and add the old one to a zip file
            // (Either the open one of if that's too large, a new one)
            if (currentPartByteStream.size() + data.length > totalExpandedBytesInEntryAllowed) {
                if (fileFull(currentPartByteStream)) {
                    resetZipFile();
                }
                addPartToFile();
            }
            currentPartByteStream.write(data);
        } catch (Exception ex) {
            String err = "Unable to create file output stream for contract " + contractNumber + "[" + (currentZipIteration - 1) + "]";
            log.error(err, ex);
            throw new IOException(err, ex);
        } finally {
            dataFileLock.unlock();
        }
    }

    /**
     * Verify that the streams have been initialized
     *
     * @throws FileNotFoundException - if there was an error writing to the file system
     */
    private void checkInitStreams() throws FileNotFoundException {
        if (getCurrentStream() == null) {
            setCurrentStream(createStream());
        }
        if (currentPartByteStream == null) {
            currentPartByteStream = new ByteArrayOutputStream();
        }
    }

    /**
     * Given a ZipEntry, add it to the zip file. At this point, you can determine the compression
     * achieved after adding the entry. We keep a running average so we can predict whether we should add
     * an entry to a zip file or create a new zip file. Before we have any values, we assume no compression.
     * Compression is measured at how small the resulting entry is compared to the size of the data before
     * compression. For example, 0.5 means that the data in the zip file is half the size of the data
     * entered. No compression would have a value of 1.
     *
     * @throws IOException - if there was a problem writing to the zip file
     */
    private void addPartToFile() throws IOException {
        ZipEntry entry = createFilePart();
        ((ZipOutputStream) getCurrentStream()).putNextEntry(entry);
        getCurrentStream().write(currentPartByteStream.toByteArray());
        ((ZipOutputStream) getCurrentStream()).closeEntry();
        setTotalBytesWritten(getTotalBytesWritten() + entry.getCompressedSize());
        double currentCompression = (double) entry.getCompressedSize() / currentPartByteStream.size();
        if (counter == 2) {
            averageCompression = currentCompression;
        } else {
            averageCompression = (averageCompression * (counter - 1) + currentCompression) / counter;
        }
        currentPartByteStream = new ByteArrayOutputStream();
    }

    /**
     * Close the old zip file and return a new zip stream
     *
     * @throws IOException - if there was an error closing/creating the file
     */
    private void resetZipFile() throws IOException {
        getCurrentStream().close();
        logManager.log(EventUtils.getFileEvent(job, currentFile, FileEvent.FileStatus.CLOSE));
        setCurrentStream(createStream());
    }

    /**
     * Determine if the zip file is full and can't accept the byte stream without going over
     * the limit
     *
     * @param ba - the byte stream you want to add
     * @return true if the additional data is projected to over fill the zip file (based on average compression)
     */
    private boolean fileFull(ByteArrayOutputStream ba) {
        double probableSize = ba.size() * averageCompression;
        return getTotalBytesWritten() + probableSize > getTotalBytesAllowed();
    }

    /**
     * Close the stream and add any lingering data to the zip file or a new one if it's too big
     */
    @Override
    public void close() throws IOException {
        if (getCurrentStream() == null) {
            return;
        }
        try {
            if (currentPartByteStream.size() != 0) {
                if (fileFull(currentPartByteStream)) {
                    resetZipFile();
                }
                addPartToFile();
            }
            logManager.log(EventUtils.getFileEvent(job, currentFile, FileEvent.FileStatus.CLOSE));
            getCurrentStream().close();
            int numFiles = filesCreated.size();
            if (filesCreated.get(numFiles - 1).toFile().length() == 0) {
                filesCreated.remove(numFiles - 1);
            }
        } catch (Exception ex) {
            String error = "Unable to close output stream for contract " + contractNumber + "[" + currentZipIteration + "]";
            log.error(error, ex);
            throw new IOException(error, ex);
        }
    }
}
