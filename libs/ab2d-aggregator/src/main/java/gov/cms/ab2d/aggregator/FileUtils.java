package gov.cms.ab2d.aggregator;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gov.cms.ab2d.aggregator.FileOutputType.getFileType;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * We work a lot with files so having a utils class to help with that is useful
 */
@Slf4j
public final class FileUtils {
    private FileUtils() {
    }

    /**
     * Given a list of files, combine them into an outfile
     *
     * @param filesToCombine - the files to combine
     * @param outFileName - the output file name with location
     * @throws IOException - if we have any IO funny business
     */
    public static void combineFiles(List<File> filesToCombine, String outFileName) throws IOException {
        Path outFile = Paths.get(outFileName);
        try (FileChannel out = FileChannel.open(outFile, CREATE, WRITE)) {
            for (File file : filesToCombine) {
                Path inFile = Paths.get(file.getAbsolutePath());
                try (FileChannel in = FileChannel.open(inFile, READ)) {
                    // For the length of the file, transfer into the output file
                    for (long p = 0, l = in.size(); p < l;)
                        p += in.transferTo(p, l - p, out);
                }
            }
        }
    }

    /**
     * Delete a list of files
     *
     * @param files - The files to delete
     * @return true if all the files were deleted
     */
    public static boolean cleanUpFiles(List<File> files) {
        boolean clean = true;
        if (files == null || files.isEmpty()) {
            return true;
        }
        for (File f : files) {
            try {
                Files.delete(Path.of(f.getAbsolutePath()));
            } catch (IOException e) {
                log.error("Unable to delete file: " + f.getAbsolutePath(), e);
                clean = false;
            }
        }
        return clean;
    }

    /**
     * Create a directory and any intermediary directories if needed
     *
     * @param dir - the directory to create
     * @return the created directory
     * @throws IOException if we can't create it
     */
    public static File createADir(String dir) throws IOException {
        File file = new File(dir);
        if (!file.exists()) {
            return Files.createDirectories(Path.of(file.getAbsolutePath())).toFile();
        }
        return file;
    }

    /**
     * Retrieve the data or error files in a driectory and get the total size of all of those files
     *
     * @param dirName - the directory to search
     * @param type - type of file
     * @return the total size of the found files
     */
    public static long getSizeOfFiles(String dirName, FileOutputType type) {
        return getSizeOfFiles(listFiles(dirName, type));
    }

    /**
     * Given a list of files, find the total size of them
     *
     * @param files - the files to check
     * @return the total size
     */
    public static long getSizeOfFiles(List<File> files) {
        if (files == null) {
            return 0;
        }
        return files.stream()
                .map(File::getAbsolutePath)
                .map(Paths::get)
                .map(p -> {
                    try {
                        return Files.size(p);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .map(Number::longValue)
                .reduce(0L, Long::sum);
    }

    /**
     * Given a directory, retrieve either error or data files
     *
     * @param fileLoc - the location
     * @param type - type of file
     * @return the list of files
     */
    public static List<File> listFiles(String fileLoc, FileOutputType type) {
        File dir = new File(fileLoc);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }
        return Stream.of(files)
                .filter(f -> type == getFileType(f.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Get the size of a file
     * @param pathString - the file path
     * @return the file size
     * @throws IOException if there is an IO issue
     */
    public static long getSizeOfFileOrDirectory(String pathString) throws IOException {
        Path path = Paths.get(pathString);
        try {
            return Files.size(path);
        } catch (NoSuchFileException ex) {
            return 0;
        }
    }

    static boolean deleteAllInDir(String dir) {
        return deleteAllInDir(new File(dir));
    }

    /**
     * Delete recursively all files in a directory and the directory
     *
     * @param dir - the dir to delete
     * @return if we successfully deleted all files in that directory and the passed directory
     */
    static boolean deleteAllInDir(File dir) {
        boolean deleted = true;
        if (dir.exists()) {
            if (dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        deleted &= deleteAllInDir(f);
                    }
                }
            } else {
                return deleteIt(dir);
            }
            deleted &= deleteIt(dir);
        }
        return deleted;
    }

    static boolean deleteIt(File f) {
        try {
            Files.delete(Path.of(f.getAbsolutePath()));
            return true;
        } catch (IOException ex) {
            log.error("Unable to delete file: " + f.getAbsolutePath(), ex);
            return false;
        }
    }
}
