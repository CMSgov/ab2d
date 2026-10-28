package gov.cms.ab2d.audit;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;

public class FileUtil {

    private static final Set<String> DISALLOWED_DIRECTORIES = Set.of("/bin", "/boot", "/dev", "/etc", "/home", "/lib",
            "/opt", "/root", "/sbin", "/sys", "/usr", "/Applications", "/Library", "/Network", "/System", "/Users", "/Volumes");

    public static void delete(File file, LambdaLogger logger) {
        try {
            Files.deleteIfExists(file.toPath());
            logger.log(file.getName() + " deleted");
        } catch (IOException exception) {
            logger.log(String.format("File/directory %s could not be deleted", file.getAbsolutePath()));
        }
    }

    /**
     * Walk the tree under baseDirectory and delete every file ending with one of the suffixes that was created before
     * cutoff. Top-level directories named in skipDirectories (jobs that are still running) are left alone.
     * A directory is deleted once a file has been removed from it and it is left empty; baseDirectory never is.
     * <p>
     * Directories are streamed rather than listed so memory stays flat however many files a job wrote, and each
     * directory is checked for emptiness once rather than after every deletion (AB2D-7509).
     *
     * @return the number of files deleted
     */
    public static int deleteExpiredFiles(String baseDirectory, Instant cutoff, Set<String> skipDirectories,
                                         LambdaLogger logger, String... suffixes) {
        return deleteExpiredFiles(Path.of(baseDirectory), cutoff, skipDirectories, logger, suffixes, true);
    }

    private static int deleteExpiredFiles(Path directory, Instant cutoff, Set<String> skipDirectories,
                                          LambdaLogger logger, String[] suffixes, boolean isBase) {
        int deleted = 0;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                BasicFileAttributes attributes = readAttributes(entry);
                if (attributes == null) {
                    continue;
                }
                String name = entry.getFileName().toString();
                if (attributes.isDirectory()) {
                    if (isBase && skipDirectories.contains(name)) {
                        logger.log(entry + " skipped; job " + name + " not in terminal state");
                        continue;
                    }
                    deleted += deleteExpiredFiles(entry, cutoff, skipDirectories, logger, suffixes, false);
                } else if (Arrays.stream(suffixes).anyMatch(name::endsWith)
                        && attributes.creationTime().toInstant().isBefore(cutoff)) {
                    delete(entry.toFile(), logger);
                    deleted++;
                }
            }
        } catch (IOException exception) {
            logger.log(String.format("Directory %s could not be read: %s", directory, exception.getMessage()));
            return deleted;
        }

        if (!isBase && deleted > 0 && isEmpty(directory)) {
            delete(directory.toFile(), logger);
        }
        return deleted;
    }

    private static BasicFileAttributes readAttributes(Path path) {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException exception) {
            return null;
        }
    }

    private static boolean isEmpty(Path directory) {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            return !entries.iterator().hasNext();
        } catch (IOException exception) {
            return false;
        }
    }

    public static void validateEfsMount(String efsMount) {
        if (!efsMount.startsWith(File.separator) && improperRoot(efsMount)) {
            throw new AuditException("EFS Mount must start with a " + File.separator);
        }

        if (efsMount.length() < 5) {
            throw new AuditException("EFS mount must be at least 5 characters");
        }

        for (String directory : DISALLOWED_DIRECTORIES) {
            if (efsMount.startsWith(directory) && !efsMount.startsWith("/opt/ab2d")) {
                throw new AuditException("EFS mount must not start with a directory that contains important files");
            }
        }
    }

    public static boolean improperRoot(String efsMount) {
        return Arrays.stream(File.listRoots())
                .anyMatch(Predicate.not(root -> efsMount.startsWith(root.getAbsolutePath())));
    }

}
