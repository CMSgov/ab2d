package gov.cms.ab2d.audit;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import gov.cms.ab2d.lambdalibs.lib.PropertiesUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.Set;

import static gov.cms.ab2d.audit.FileUtil.deleteExpiredFiles;
import static gov.cms.ab2d.audit.FileUtil.validateEfsMount;

public class AuditEventHandler implements RequestStreamHandler {

    /**
     * Find all files under the EFS mount that end with .ndjson or .ndjson.gz
     * If the file's creation time is older than the configured time to live and its job is not still running, delete it
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
        Set<String> activeJobs = JobStatusRepository.findActiveJobUuids();
        log.log("Found " + activeJobs.size() + " active jobs: " + activeJobs);
        Instant cutoff = Instant.now().minus(fileTTL, ChronoUnit.HOURS);
        int deleted = deleteExpiredFiles(efs, cutoff, activeJobs, log, ".ndjson", ".ndjson.gz");
        outputStream.write("{\"status\": \"ok\" }".getBytes(StandardCharsets.UTF_8));
        log.log("Audit Lambda completed, " + deleted + " files deleted");
    }

}
