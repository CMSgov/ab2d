package gov.cms.ab2d.aggregator;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

import static gov.cms.ab2d.aggregator.Aggregator.AggregatorResult.NOT_PERFORMED;
import static gov.cms.ab2d.aggregator.FileOutputType.DATA;
import static gov.cms.ab2d.aggregator.FileOutputType.ERROR;
import static gov.cms.ab2d.aggregator.FileUtils.cleanUpFiles;
import static gov.cms.ab2d.aggregator.FileUtils.combineFiles;
import static gov.cms.ab2d.aggregator.FileUtils.getSizeOfFileOrDirectory;
import static gov.cms.ab2d.aggregator.FileUtils.getSizeOfFiles;
import static gov.cms.ab2d.aggregator.FileUtils.listFiles;

/**
 * Does the work of aggregating files
 */
@Getter
@Slf4j
public class Aggregator {

    public enum AggregatorResult {
        PERFORMED,
        NOT_PERFORMED
    }

    public static final int ONE_MEGA_BYTE = 1024 * 1024;

    private final String jobId;
    private final String mainDirectory;
    private final String contractNumber;
    private final String streamDir;
    private final String finishedDir;
    private final int maxMegaBytes;
    private final int multiplier;

    private Map<FileOutputType, Integer> fileCounts = new HashMap<>();

    /**
     * Define the Aggregator for the job
     *
     * @param jobId - the job ID
     * @param contractNumber - the contract number (used for naming files)
     * @param fileDir - The top level location where files are to be placed (before the job ID)
     * @param maxMegaBytes - The maximum size of a final file to be created
     * @param streamDir -  The location of the streaming directory
     * @param finishedDir -  The location of files that have finished streaming (but not yet aggregated)
     * @param multiplier - When to start aggregating. If the multiplier is 2 for example and the max
     *                   MegaBytes size is 200, we won't start aggregating files until there are at least
     *                   400 MB of data in the finished directory.
     */
    public Aggregator(String jobId, String contractNumber, String fileDir, int maxMegaBytes, String streamDir,
                      String finishedDir, int multiplier) {
        this.jobId = jobId;
        this.mainDirectory = Paths.get(fileDir, jobId).toFile().getAbsolutePath();
        this.contractNumber = contractNumber;
        this.streamDir = streamDir;
        this.maxMegaBytes = maxMegaBytes;
        this.finishedDir = finishedDir;
        this.multiplier = multiplier;

        FileOutputType[] fileOutputValues = FileOutputType.values();
        for (FileOutputType type : fileOutputValues) {
            fileCounts.put(type, 1);
        }

        // The worker should do this by default, but just in case, set up all the directories. If there is an error
        // ignore it
        try {
            JobHelper.workerSetUpJobDirectories(jobId, fileDir, streamDir, finishedDir);
        } catch (Exception ex) {
            log.error("Issue with aggregator creating directories", ex);
        }
    }

    /**
     * Aggregate! This method finds the best files to aggregate, combines those files
     * and deletes the temporary files created by the worker. This will only do
     * one aggregation. The goal is to run this method until it returns false
     *
     * @param fileType - type of file to aggregate
     * @return true if there are enough files to aggregate (or the worker is done writing data)
     *      and we then did aggregate, false if there aren't enough files to aggregate.
     * @throws IOException if one of this file manipulations fails
     */
    public AggregatorResult aggregate(FileOutputType fileType) throws IOException {
        // remove any empty files
        removeEmptyFiles();

        if (!okayToDoAggregation(fileType)) {
            return NOT_PERFORMED;
        }
        List<File> bestFiles = getBestFiles(fileType);
        if (bestFiles.isEmpty()) {
            return NOT_PERFORMED;
        }
        String fileName = getNextFileName(fileType);
        if (fileName == null) {
            return NOT_PERFORMED;
        }
        combineFiles(bestFiles, fileName);
        cleanUpFiles(bestFiles);
        return AggregatorResult.PERFORMED;
    }

    /**
     * Remove any files in the finished directory that is empty. This happens when the batch of beneficiaries
     * have no EOBs.
     */
    void removeEmptyFiles() {
        String fDir = this.mainDirectory + File.separator + this.finishedDir;
        List<File> availableFiles = listFiles(fDir, DATA);
        availableFiles.addAll(listFiles(fDir, ERROR));
        List<File> emptyFiles = availableFiles.stream()
                .filter(f -> {
                            long size;
                            try {
                                size = getSizeOfFileOrDirectory(f.getAbsolutePath());
                            } catch (IOException e) {
                                size = 0L;
                            }
                            return size == 0;
                        }).collect(Collectors.toList());
        emptyFiles.forEach(File::delete);
    }

    /**
     * Return the name of the next file for the type of file it is.
     * This will provide the full file path
     *
     * @param type - type of output file
     * @return the name of the next file to aggregate into
     */
    String getNextFileName(FileOutputType type) {
        String fileName = getNextFilePart(type);
        return Path.of(mainDirectory, fileName).toFile().getAbsolutePath();
    }

    /**
     * Provide the file name (without path) of the next file for a specific file type
     * @param type - the file output type
     * @return the file name
     */
    String getNextFilePart(FileOutputType type) {
        int currentVal = fileCounts.get(type);
        var paddedPartitionNo = StringUtils.leftPad("" + currentVal, 4, '0');
        fileCounts.put(type, ++currentVal);
        return contractNumber +
                "_" +
                paddedPartitionNo +
                type.getSuffix();
    }

    /**
     * If we should aggregate files. The answer is true if the size of the data in the finished is greater
     * than the max size of the file times the multiplier or if the worker is done streaming data.
     *
     * @param type - type of file
     * @return - true if we have enough files or the worker is done writing out files
     */
    boolean okayToDoAggregation(FileOutputType type) {
        long size = getSizeOfFiles(this.mainDirectory + File.separator + this.finishedDir, type);
        return (size > ((long) this.multiplier * getMaxFileSize())) || isJobDoneStreamingData();
    }

    /**
     * We attempt to find the best combination of files to aggregate. This isn't a complicated
     * algorithm. It sorts the files by size and grabs files from largest to smallest. If it gets
     * to a file that will make it exceed the max size, it skips that file and goes to the next file
     * and attempts to include that. It continues down the entire list.
     *
     * For example, if we * had a list with file sizes: 9, 5, 3, 2 and we had a max size of 13,
     * we'd first grab 9, but 5 would put us over the top so we skip it, we'd keep 3 because that
     * wouldn't be more than * 13, but the next value 2 puts us over the top. There are probably
     * better algorithms but this gets the job done in a reasonably efficient manner.
     *
     * @param type type of file
     * @return the list of "best" files to combine to optimize fullness of individual files
     */
    List<File> getBestFiles(FileOutputType type) {
        // Get all the files and their sizes in sorted order
        List<FileReferenceHolder> sortedFiles = getSortedFileReferences(type);

        // If there are no files, return an empty list
        if (sortedFiles == null || sortedFiles.isEmpty()) {
            return new ArrayList<>();
        }

        // If there is only one file, return it - we have no other choice
        if (sortedFiles.size() == 1) {
            return List.of(sortedFiles.get(0).getFile());
        }

        // If the largest file is larger than the max file size, it's not great but should
        // be returned
        if (sortedFiles.get(sortedFiles.size() - 1).getSize() > getMaxFileSize()) {
            return List.of(sortedFiles.get(sortedFiles.size() - 1).getFile());
        }

        List<FileReferenceHolder> bestFiles = new ArrayList<>();
        long totalFileSize = 0L;

        // Add the large files first - iterate backwards until we're over the top on the next item
        // Remove items as we go. Find the first next biggest files that will add up to the max file size
        ListIterator<FileReferenceHolder> bigIter = sortedFiles.listIterator(sortedFiles.size());
        while (bigIter.hasPrevious()) {
            FileReferenceHolder fd = bigIter.previous();
            if (totalFileSize + fd.getSize() > getMaxFileSize()) {
                continue;
            }
            totalFileSize += fd.getSize();
            bestFiles.add(fd);
            bigIter.remove();
        }

        return bestFiles.stream().map(FileReferenceHolder::getFile).collect(Collectors.toList());
    }

    /**
     * Take the list of files in a directory and order them by size
     *
     * @param files - the files to sort
     * @return - the list of ordered files
     */
    List<FileReferenceHolder> orderBySize(List<FileReferenceHolder> files) {
        if (files == null) {
            return new ArrayList<>();
        }
        return files.stream().sorted(Comparator.comparingLong(FileReferenceHolder::getSize)).collect(Collectors.toList());
    }

    /**
     * Get file descriptors for each file. This allows us to do comparisons and sorting by
     * file size without having to check with the file system each time
     *
     * @param type - type of file
     * @return the list of file descriptors with the relevant extensions
     */
    List<FileReferenceHolder> getSortedFileReferences(FileOutputType type) {
        List<File> availableFiles = listFiles(this.mainDirectory + File.separator + this.finishedDir, type);

        List<FileReferenceHolder> files = new ArrayList<>();
        availableFiles.forEach(f -> {
            long size;
            try {
                size = getSizeOfFileOrDirectory(f.getAbsolutePath());
            } catch (IOException e) {
                size = 0L;
            }
            files.add(new FileReferenceHolder(f, size));
        });

        // Order the files by file size
        return orderBySize(files);
    }

    /**
     * Returns true if the job has finished streaming data. You know this because the streaming directory
     * no longer exists
     *
     * @return if the job is done writing data
     */
    public boolean isJobDoneStreamingData() {
        String streamingDir = this.mainDirectory + File.separator + this.streamDir;
        boolean fileExists = dirExists(streamingDir);
        // Job is done if dir doesn't exist
        return !fileExists;
    }

    private boolean dirExists(String dir) {
        return Files.exists(Path.of(dir));
    }

    /**
     * Has the aggregator finished doing all its aggregation?
     *
     * @return true if the aggregator has indicated that it has finished combining all outputted worker files.
     * This will always be false if the worker is not done streaming
     */
    public boolean isJobAggregated() {
        // If job isn't done, we can't be done aggregating
        if (dirExists(this.mainDirectory + File.separator + this.streamDir)) {
            return false;
        }
        // Look for the files in the done writing directory
        return !dirExists(this.mainDirectory + File.separator + this.finishedDir);
    }

    public int getMaxFileSize() {
        return this.maxMegaBytes * ONE_MEGA_BYTE;
    }
}
