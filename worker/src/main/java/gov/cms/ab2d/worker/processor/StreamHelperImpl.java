package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.eventlogger.LogManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.file.StandardOpenOption.APPEND;

/**
 * Contains the common methods for other StreamHelper implementations
 */
@Slf4j
public abstract class StreamHelperImpl implements StreamHelper, AutoCloseable {
    public enum FileOutputType {
        NDJSON(".ndjson"),
        NDJSON_ERROR("_error.ndjson"),
        ZIP(".zip");

        private String suffix;
        FileOutputType(String suffix) {
            this.suffix = suffix;
        }
        public String getSuffix() {
            return suffix;
        }
    }

    @Getter
    private LogManager logManager;

    @Getter
    private Job job;

    // Current file counter
    @Getter
    private volatile int counter = 1;

    // Passed contract number
    @Getter
    private final String contractNumber;

    // Directory where to put the files
    @Getter
    private final Path path;

    // Total number of bytes written to the file
    @Getter @Setter
    private volatile long totalBytesWritten = 0;

    // Total bytes allowed in the file
    @Getter
    private final long totalBytesAllowed;

    // The current output stream
    @Getter @Setter
    private volatile OutputStream currentStream;

    // The time before a lock times out and unlocks
    private final int tryLockTimeout;

    // Data file lock
    @Getter
    private final Lock dataFileLock  = new ReentrantLock();

    // Error file lock
    @Getter
    private final Lock errorFileLock = new ReentrantLock();

    // List of data files created
    @Getter
    private List<Path> filesCreated;

    // List of error files created
    @Getter
    private List<Path> errorFilesCreated;

    // Location of error file
    private Path errorFile;

    /**
     * Main constructor
     *
     * @param path - Where to create the files
     * @param contractNumber - the contract number
     * @param totalBytesAllowed - the total number of bytes allowed in a file
     * @param tryLockTimeout - the lock time out
     */
    StreamHelperImpl(Path path, String contractNumber, long totalBytesAllowed, int tryLockTimeout,
                     LogManager logManager, Job job) {
        this.path = path;
        this.contractNumber = contractNumber;
        this.totalBytesAllowed = totalBytesAllowed;
        this.tryLockTimeout = tryLockTimeout;
        this.filesCreated = new ArrayList<>();
        this.errorFilesCreated = new ArrayList<>();
        this.logManager = logManager;
        this.job = job;
    }

    /**
     * Create the next file name in the sequence
     *
     * @return the file name
     */
    String createFileName() {
        var partName = Integer.toString(counter);
        var paddedPartitionNo = StringUtils.leftPad(partName, 4, '0');
        counter++;
        return contractNumber +
                "_" +
                paddedPartitionNo +
                FileOutputType.NDJSON.getSuffix();
    }

    /**
     * Lock the resource with the time out
     *
     * @param lock - the lock
     */
    void tryLock(Lock lock) {
        final String errMsg = "Terminate processing. Unable to acquire lock";
        try {
            final boolean lockAcquired = lock.tryLock(tryLockTimeout, TimeUnit.SECONDS);
            if (!lockAcquired) {
                final String errMsg1 = errMsg + " after waiting " + tryLockTimeout + " seconds.";
                throw new RuntimeException(errMsg1);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(errMsg);
        }
    }

    /**
     * Take an array of bytes and append it to a file
     *
     * @param outputFile - the file to append to
     * @param data - the data to write
     */
    void appendToFile(Path outputFile, byte[] data) {
        try {
            Files.write(outputFile, data, APPEND);
        } catch (IOException e) {
            var errMsg = "Could not write to output file";
            log.error("{}: {} ", errMsg, outputFile.toAbsolutePath(), e);
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes a chunk of data to the error file. Roll-over not supported at this point but MAY
     * BE required in future if we determine errors files are getting too big.
     * This method MUST be thread-safe and do appropriate locking, e.g. using ReentrantLock
     *
     * @param data - the error data to add
     */
    @Override
    public void addError(String data) throws IOException {
        if (data == null || data.isEmpty()) {
            return;
        }
        tryLock(errorFileLock);
        try {
            if (errorFile == null) {
                createErrorFile();
            }
            appendToFile(errorFile, data.getBytes(StandardCharsets.UTF_8));
        } finally {
            errorFileLock.unlock();
        }
    }

    /**
     * If the error file doesn't exist, create it
     */
    void createErrorFile() {
        var fileName = contractNumber +
                FileOutputType.NDJSON_ERROR.getSuffix();

        final Path errorFilePath = Path.of(path.toString(), fileName);
        try {
            errorFile = Files.createFile(errorFilePath);
        } catch (IOException e) {
            var errMsg = "Could not create output error file : ";
            log.error("{} {} ", errMsg, errorFilePath.toAbsolutePath(), e);
            throw new UncheckedIOException(e);
        }

        errorFilesCreated.add(errorFile);
    }

    /**
     * Return the list of files created
     *
     * @return the files
     */
    @Override
    public List<Path> getDataFiles() {
        return filesCreated;
    }

    /**
     * Return the error files (currently just one)
     *
     * @return the error files
     */
    @Override
    public List<Path> getErrorFiles() {
        return errorFilesCreated;
    }
}
