package gov.cms.ab2d.audit;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;

public class FileUtil {

    private static final Set<String> DISALLOWED_DIRECTORIES = Set.of("/bin", "/boot", "/dev", "/etc", "/home", "/lib",
            "/opt", "/root", "/sbin", "/sys", "/usr", "/Applications", "/Library", "/Network", "/System", "/Users", "/Volumes");

    /**
     * @return false if the file could not be deleted
     */
    public static boolean delete(File file, LambdaLogger logger) {
        try {
            Files.deleteIfExists(file.toPath());
            logger.log(file.getName() + " deleted");
            return true;
        } catch (IOException exception) {
            logger.log(String.format("File/directory %s could not be deleted", file.getAbsolutePath()));
            return false;
        }
    }

    public record CleanupResult(int deleted, int failures) {
    }

    /**
     * Delete every file under directory that ends with one of the suffixes and was created before cutoff, skipping
     * directories named in skipDirectories (jobs still running). A subdirectory left empty is deleted if this run
     * emptied it or it is itself older than cutoff; the age check keeps a job that started after the active job
     * lookup from losing its new, empty directory. directory itself is never deleted.
     * <p>
     * Directories are streamed rather than listed so memory stays flat however many files a job wrote (AB2D-7509).
     * Failures are counted rather than thrown so one bad file does not stop the rest of the cleanup.
     */
    public static CleanupResult deleteExpiredFiles(Path directory, Instant cutoff, Set<String> skipDirectories,
                                                   LambdaLogger logger, String... suffixes) {
        int deleted = 0;
        int failures = 0;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                String name = entry.getFileName().toString();
                BasicFileAttributes attributes;
                try {
                    attributes = Files.readAttributes(entry, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                } catch (NoSuchFileException exception) {
                    continue; // removed since it was listed
                }
                boolean expired = attributes.creationTime().toInstant().isBefore(cutoff);

                if (attributes.isDirectory() && skipDirectories.contains(name)) {
                    logger.log(entry + " skipped; job " + name + " not in terminal state");
                } else if (attributes.isDirectory()) {
                    CleanupResult result = deleteExpiredFiles(entry, cutoff, skipDirectories, logger, suffixes);
                    deleted += result.deleted();
                    failures += result.failures();
                    if ((result.deleted() > 0 || expired) && isEmpty(entry) && !delete(entry.toFile(), logger)) {
                        failures++;
                    }
                } else if (expired && Arrays.stream(suffixes).anyMatch(name::endsWith)) {
                    if (delete(entry.toFile(), logger)) {
                        deleted++;
                    } else {
                        failures++;
                    }
                }
            }
        } catch (IOException exception) {
            logger.log(String.format("Directory %s could not be read: %s", directory, exception.getMessage()));
            failures++;
        }
        return new CleanupResult(deleted, failures);
    }

    private static boolean isEmpty(Path directory) throws IOException {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            return !entries.iterator().hasNext();
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
