package gov.cms.ab2d.audit.cleanup;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.service.JobService;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import gov.cms.ab2d.common.util.EventUtils;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.eventlogger.reports.sql.DoSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class FileDeletionServiceImpl implements FileDeletionService {

    @Value("${efs.mount}")
    private String efsMount;

    @Value("${audit.files.ttl.hours}")
    private int auditFilesTTLHours;

    private final JobService jobService;
    private final LogManager eventLogger;
    private final DoSummary doSummary;

    private static final String FILE_EXTENSION = ".ndjson";

    private static Set<String> disallowedDirectories = Set.of("/bin", "/boot", "/dev", "/etc", "/home", "/lib",
            "/opt", "/root", "/sbin", "/sys", "/usr", "/Applications", "/Library", "/Network", "/System", "/Users", "/Volumes");

    public FileDeletionServiceImpl(JobService jobService, LogManager eventLogger, DoSummary doSummary) {
        this.jobService = jobService;
        this.eventLogger = eventLogger;
        this.doSummary = doSummary;
    }

    /**
     * Delete all files that are in the efs mount with the .ndjson extension that are older than the TTL variable
     */
    @Override
    public void deleteFiles() {
        log.info("File deletion service kicked off");
        validateEfsMount();

        try (Stream<Path> filePaths = Files.walk(Paths.get(efsMount), FileVisitOption.FOLLOW_LINKS)) {
            List<Path> validFiles = filePaths.filter(Files::isRegularFile).collect(Collectors.toList());
            // The list of jobs that were expired
            Set<String> jobsDeleted = new HashSet<>();
            for (Path file : validFiles) {
                // Get the JobId from the name
                var jobUuid = new File(file.toUri()).getParentFile().getName();
                // See if the file should be deleted
                boolean deleted = deleteFile(file);
                if (deleted) {
                    // If it was deleted
                    jobsDeleted.add(jobUuid);
                }
            }
            for (String job : jobsDeleted) {
                eventLogger.log(LogManager.LogType.KINESIS, doSummary.getSummary(job));
            }
        } catch (IOException e) {
            log.error("Encountered exception while trying to gather the list of files to delete", e);
        }
    }

    /**
     * validates the EFS mount.
     */
    private void validateEfsMount() {
        if (!efsMount.startsWith("/")) {
            throw new EFSMountFormatException("EFS Mount must start with a /");
        }

        if (efsMount.length() < 5) {
            throw new EFSMountFormatException("EFS mount must be at least 5 characters");
        }

        for (String directory : disallowedDirectories) {
            if (efsMount.startsWith(directory) && !efsMount.startsWith("/opt/ab2d")) {
                throw new EFSMountFormatException("EFS mount must not start with a directory that contains important files");
            }
        }
    }

    /**
     * Given a file path, checks to make sure it is eligible for deletion and then deletes it.
     *
     * @param path - path of the file to be deleted
     * @return true if it is a valid job and it was deleted
     */
    private boolean deleteFile(Path path) {
        var jobUuid = new File(path.toUri()).getParentFile().getName();
        var job = findJob(jobUuid, path);

        boolean deletedJobFile = false;
        FileEvent fileEvent = null;
        try {
            var deleteCheckTime = getDeleteCheckTime(path, job);
            if (deleteCheckTime == null) {
                logFileNotEligibleForDeletion(path);
            } else {
                final Instant oldestDeletableTime = calculateOldestDeletableTime();
                final boolean filenameHasValidExtension = isFilenameExtensionValid(path);

                if (deleteCheckTime.isBefore(oldestDeletableTime) && filenameHasValidExtension) {
                    // Create the event here while we still have the file data
                    fileEvent = EventUtils.getFileEvent(job, new File(path.toUri()), FileEvent.FileStatus.DELETE);

                    Files.delete(path);
                    log.info("Deleted file {}", path);
                    if (job != null) {
                        deletedJobFile = true;
                    }
                } else {
                    logFileNotEligibleForDeletion(path);
                }
            }
            // Actually log here because if we've gotten here with an exception, we deleted it.
            if (fileEvent != null) {
                eventLogger.log(fileEvent);
            }
        } catch (IOException e) {
            log.error("Encountered exception trying to delete a file {}, moving onto next one", path, e);
        }
        return deletedJobFile;
    }

    /**
     * Given a jobUuid, finds the job
     *
     * @param jobUuid - the job id
     * @param path - the location of the file
     * @return the Job object
     */
    private Job findJob(String jobUuid, Path path) {
        Job job = null;
        try {
            job = jobService.getJobByJobUuid(jobUuid);
        } catch (ResourceNotFoundException e) {
            log.trace("No job connected to directory {}", path); // Put a log statement here to make PMD happy
        }
        return job;
    }

    /**
     * Calculates the oldest deletable dateTime given the time to live for the audit files.
     *
     * @return oldest deletable time
     */
    private Instant calculateOldestDeletableTime() {
        return Instant.now().minus(auditFilesTTLHours, ChronoUnit.HOURS);
    }

    /**
     * Returns true if the file has the proper file extension
     *
     * @param path - the file
     * @return true if the filename has a valid extension (.ndjson)
     */
    private boolean isFilenameExtensionValid(Path path) {
        return path.toString().endsWith(FILE_EXTENSION.toLowerCase());
    }


    private Instant getDeleteCheckTime(Path path, Job job)  throws IOException {
        Instant deleteCheckTime = null;
        if (job != null && job.getStatus().isFinished()) {
            deleteCheckTime = job.getCompletedAt().toInstant();
        } else if (job == null) {
            FileTime creationTime = (FileTime) Files.getAttribute(path, "creationTime");
            deleteCheckTime = creationTime.toInstant();
        }
        return deleteCheckTime;
    }

    private void logFileNotEligibleForDeletion(Path path) {
        log.info("File not eligible for deletion {}", path);
    }
}
