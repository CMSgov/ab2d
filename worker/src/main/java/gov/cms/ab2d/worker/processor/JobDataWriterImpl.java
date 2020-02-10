package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Contract;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.file.StandardOpenOption.APPEND;

/**
 * Since the Worker processes contracts sequentially, create a new instance for each contract,
 * and make sure only that single instance is shared among the threads.
 */
@Slf4j
public class JobDataWriterImpl implements JobDataWriter {
    static final String OUTPUT_FILE_SUFFIX = ".ndjson";
    private static final String ZIP_FILE_SUFFIX = ".zip";
    private static final String ERROR_FILE_SUFFIX = "_error.ndjson";

    private final Path outputDir;
    private final Contract contract;
    private final long maxFileSize;
    private final int tryLockTimeout;
    private final String fileSuffix;

    private final Lock dataFileLock  = new ReentrantLock();
    private final Lock errorFileLock = new ReentrantLock();

    private Path errorFile;
    private volatile Path dataFile;
    private final List<Path> dataFiles  = new ArrayList<>();
    private final List<Path> errorFiles = new ArrayList<>();

    private volatile int partitionCounter;


    public JobDataWriterImpl(Path outputDir, Contract contract, int tryLockTimeout, long maxFileSize,
                             String fileSuffix) {
        this.outputDir = outputDir;
        this.contract = contract;
        this.tryLockTimeout = tryLockTimeout;
        this.maxFileSize = maxFileSize;
        this.fileSuffix = fileSuffix;
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
        tryLock(dataFileLock);
        try {
            if (dataFile == null) {
                createNewDataFile();
            }

            if (dataFile.toFile().length() + data.length > maxFileSize) {
                createNewDataFile();
            }

            appendToFile(dataFile, data);
        } finally {
            dataFileLock.unlock();
        }

    }

    private void createNewDataFile() {
        ++partitionCounter;

        var partName = Integer.toString(partitionCounter);
        var paddedPartitionNo = StringUtils.leftPad(partName, 4, '0');

        var fileName = new StringBuilder()
                .append(contract.getContractNumber())
                .append("_")
                .append(paddedPartitionNo)
                .append(OUTPUT_FILE_SUFFIX)
                .toString();

        validateOutputDir();
        final Path dataFilePath = Path.of(outputDir.toString(), fileName);
        try {
            dataFile = Files.createFile(dataFilePath);
        } catch (IOException e) {
            var errMsg = "Could not create output data file : ";
            log.error("{} {} ", errMsg, dataFilePath.toAbsolutePath(), e);
            throw new UncheckedIOException(e);
        }

        log.info("Created new Data file : {}", dataFile.toAbsolutePath());
        dataFiles.add(dataFile);
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
        tryLock(errorFileLock);
        try {
            if (errorFile == null) {
                createErrorFile();
            }

            appendToFile(errorFile, data);
        } finally {
            errorFileLock.unlock();
        }

    }

    private void createErrorFile() {
        var fileName = new StringBuilder()
                .append(contract.getContractNumber())
                .append(ERROR_FILE_SUFFIX)
                .toString();

        validateOutputDir();
        final Path errorFilePath = Path.of(outputDir.toString(), fileName);
        try {
            errorFile = Files.createFile(errorFilePath);
        } catch (IOException e) {
            var errMsg = "Could not create output error file : ";
            log.error("{} {} ", errMsg, errorFilePath.toAbsolutePath(), e);
            throw new UncheckedIOException(e);
        }

        errorFiles.add(errorFile);
    }

    private void tryLock(Lock lock) {
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

    private void validateOutputDir() {
        if (outputDir == null) {
            throw new IllegalArgumentException("output directory must not be null");
        }
    }


    private void appendToFile(Path outputFile, byte[] data) {
        try {
            Files.write(outputFile, data, APPEND);
        } catch (IOException e) {
            var errMsg = "Could not write to output file";
            log.error("{}: {} ", errMsg, outputFile.toAbsolutePath(), e);
            throw new UncheckedIOException(e);
        }
    }


    /**
     * Once the job is all done, we call this method to find out what output files have been
     * created.
     */
    @Override
    public List<Path> getDataFiles() {
        return dataFiles;
    }

    /**
     * Once the job is all done, we call this method to find out what error files have been
     * created, if any.
     */
    @Override
    public List<Path> getErrorFiles() {
        return errorFiles;
    }
}
