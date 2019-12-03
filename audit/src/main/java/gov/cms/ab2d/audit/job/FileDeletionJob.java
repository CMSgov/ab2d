package gov.cms.ab2d.audit.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

@Slf4j
public class FileDeletionJob implements Job {

    public static final String EFS_MOUNT = "efsMount";

    public static final String AUDIT_FILES_TTL_HOURS = "auditFilesTTLHours";

    /**
     * Delete all files that are in the efs mount that are older than the TTL variable
     * @param jobExecutionContext
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        try (Stream<Path> walk = Files.walk(Paths.get(jobExecutionContext.getJobDetail().getJobDataMap().getString(EFS_MOUNT)))) {
            walk.filter(Files::isRegularFile).forEach(path -> {
                try {
                    FileTime creationTime = (FileTime) Files.getAttribute(path, "creationTime");
                    if (creationTime.toInstant().isBefore(Instant.now().minus(
                            jobExecutionContext.getJobDetail().getJobDataMap().getInt(AUDIT_FILES_TTL_HOURS), ChronoUnit.HOURS))) {
                        Files.delete(path);
                        log.info("Deleted file {}", path);
                    }
                } catch (IOException e) {
                    log.error("Encountered exception trying to delete a file, moving onto next one", e);
                }
            });

        } catch (IOException e) {
            log.error("Encountered exception while trying to gather the list of files to delete", e);
        }
    }
}
