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
     * Walk the tree under baseDirectory and delete every file ending with endsWith that was created before cutoff.
     * @return the number of files deleted
     */
    public static int deleteExpiredFiles(String baseDirectory, String endsWith, Instant cutoff, LambdaLogger logger) {
        return deleteExpiredFiles(Path.of(baseDirectory), endsWith, cutoff, logger, true);
    }

    private static int deleteExpiredFiles(Path directory, String endsWith, Instant cutoff, LambdaLogger logger,
                                          boolean isBase) {
        int deleted = 0;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                BasicFileAttributes attributes = readAttributes(entry);
                if (attributes == null) {
                    continue;
                }
                if (attributes.isDirectory()) {
                    deleted += deleteExpiredFiles(entry, endsWith, cutoff, logger, false);
                } else if (entry.getFileName().toString().endsWith(endsWith)
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
