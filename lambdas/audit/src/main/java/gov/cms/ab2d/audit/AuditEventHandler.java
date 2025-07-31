package gov.cms.ab2d.audit;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import gov.cms.ab2d.lambdalibs.lib.PropertiesUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import static gov.cms.ab2d.audit.FileUtil.delete;
import static gov.cms.ab2d.audit.FileUtil.findAllMatchingFilesAndParentDirs;
import static gov.cms.ab2d.audit.FileUtil.findMatchingDirectories;
import static gov.cms.ab2d.audit.FileUtil.validateEfsMount;

public class AuditEventHandler implements RequestStreamHandler {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("[\\da-f]{8}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{12}");

    /**
     * Find all folders with uuids as their name, within those folders find all files that end with .ndjson
     * If the file is older than the configured time to live, delete it
     * If the containing folder is now empty, delete it too
     *
     * @param inputStream  - AWS supplied input stream which contains whatever message the upstream system sent
     * @param outputStream - AWS expects a json message to be written to this stream before this method ends
     * @param context      - Some objects and information provided by AWS.
     *                     Of particular interests is the provided logger instance.
     */
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger log = context.getLogger();
        log.log("Audit Lambda started");
        Properties properties = PropertiesUtil.loadProps();
        int fileTTL = Integer.parseInt(properties.getProperty("audit_files_ttl_hours"));
        String efs = properties.getProperty("AB2D_EFS_MOUNT");
        validateEfsMount(efs);
        Set<File> files = findMatchingDirectories(efs, UUID_PATTERN);
        findAllMatchingFilesAndParentDirs(efs, files, ".ndjson");
        files.forEach(file -> deleteExpired(file, fileTTL, log));
        outputStream.write("{\"status\": \"ok\" }".getBytes(StandardCharsets.UTF_8));
        log.log("Audit Lambda completed");
    }

    private void deleteExpired(File file, int fileTTL, LambdaLogger log) {
        try {
            if (file.exists() && fileOldEnough(file, fileTTL)) {
                File dir = null;
                if (!file.isDirectory()) {
                    dir = file.getParentFile();
                    delete(file, log);
                }
                if (dir != null && dir.isDirectory() && Optional.ofNullable(dir.listFiles())
                        .orElse(new File[]{}).length == 0) {
                    delete(dir, log);
                }
            }
        } catch (IOException e) {
            throw new AuditException(e);
        }
    }

    /**
     * If a file's creation time is older than the current time minus our file time to live return true
     *
     * @param file    - The file to check
     * @param fileTTL - The maximum age of a file based on its creationTime
     * @return - True if a file is older, false if not
     * @throws IOException - Throws when the file doesn't exist
     */
    private boolean fileOldEnough(File file, int fileTTL) throws IOException {
        return ((FileTime) Files.getAttribute(file.getAbsoluteFile()
                .toPath(), "creationTime")).toInstant()
                .isBefore(Instant.now()
                        .minus(fileTTL, ChronoUnit.HOURS));
    }

}
