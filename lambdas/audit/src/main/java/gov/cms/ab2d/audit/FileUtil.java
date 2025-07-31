package gov.cms.ab2d.audit;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    public static void findAllMatchingFilesAndParentDirs(String directoryName, Set<File> files, String endsWith) {
        File directory = new File(directoryName);
        // Get all files from a directory.
        File[] fList = directory.listFiles();
        if (fList != null)
            for (File file : fList) {
                if (file.getName()
                        .endsWith(endsWith)) {
                    files.add(file);
                }
                if (file.isDirectory()) {
                    findAllMatchingFilesAndParentDirs(file.getAbsolutePath(), files, endsWith);
                }
            }
    }

    public static Set<File> findMatchingDirectories(String baseDirectory, Pattern pattern) {
        File directory = new File(baseDirectory);
        File[] fList = directory.listFiles();
        return Arrays.stream(Optional.ofNullable(fList)
                        .orElse(new File[]{}))
                .filter(f -> pattern.matcher(f.getName())
                        .matches())
                .collect(Collectors.toSet());
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
