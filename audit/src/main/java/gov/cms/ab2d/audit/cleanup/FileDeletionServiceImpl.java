package gov.cms.ab2d.audit.cleanup;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private static final Set<String> DISALLOWED_DIRECTORIES = Set.of("/bin", "/boot", "/dev", "/etc", "/home", "/lib",
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

            List<Path> validFiles = new ArrayList<>();
            List<Path> directories = new ArrayList<>();

            // Split into regular files
            // and writable directories
            filePaths.forEach(path -> {
                if (Files.isRegularFile(path)) {
                    validFiles.add(path);
                } else if (Files.isDirectory(path) && Files.isWritable(path)) {
                    directories.add(path);
                }
            });

            deleteExpiredJobFiles(validFiles);
            deleteEmptyDirectories(directories);

        } catch (IOException e) {
            log.error("Encountered exception while trying to gather the list of files to delete", e);
        }
    }

    void deleteEmptyDirectories(List<Path> emptyDirectories) {
        for (Path directory : emptyDirectories) {
            try {
                if (isEmptyDirectory(directory) && isExpiredDirectory(directory)) {
                    Files.delete(directory);

                    // Log deleting a folder
                    var jobUuid = new File(directory.toUri()).getName();
                    log.info("delete directory {} for job {}", directory.toUri().toString(), jobUuid);
                } else {
                    logFolderNotEligibleForDeletion(directory);
                }
            } catch (Exception exception) {
                log.error("failed to delete or process a directory {}", directory.toUri().toString(), exception);
            }
        }
    }

    void deleteExpiredJobFiles(List<Path> validFiles) {
        // The list of jobs that were expired
        Set<String> jobsDeleted = new HashSet<>();
        for (Path file : validFiles) {

            try {
                // Get the JobId from the name
                var jobUuid = getJobFromFile(file);
                // See if the file should be deleted
                boolean deleted = deleteFile(file);
                if (deleted) {
                    // If it was deleted add it to the list of jobs with deleted files
                    jobsDeleted.add(jobUuid);
                }
            } catch (Exception exception) {
                log.error("failed to delete or process a regular file {}", file.toUri().toString(), exception);
            }
        }
        for (String job : jobsDeleted) {
            eventLogger.log(LogManager.LogType.KINESIS, doSummary.getSummary(job));
        }
    }

    private String getJobFromFile(Path file) {
        return new File(file.toUri()).getParentFile().getName();
    }

    /**
     * Check whether directory contains any files
     * @param directory directory to check which must exist
     * @return true if a file is found in directory
     * @throws IOException on failure to read directory
     */
    private boolean isEmptyDirectory(Path directory) throws IOException {
        // Lazily look for first child
        try (Stream<Path> children =  Files.list(directory)) {
            return children.findAny().isEmpty();
        }
    }

    /**
     * Check whether a directory is associated with a job, if the directory is check that the job has completed more
     * than x hours ago.
     * @param directory directory to check
     * @return true if directory belongs to a job that has finished long enough ago
     * @throws IOException should not throw ever because getDeleteCheckTime does not hit edge case
     */
    private boolean isExpiredDirectory(Path directory) throws IOException {
        var jobUuid = new File(directory.toUri()).getName();
        var job = findJob(jobUuid, directory);

        if (job == null) {
            return false;
        }

        // Does not throw IO Exception because else clause is never hit
        Instant deletedAt = getDeleteCheckTime(directory, job);
        if (deletedAt == null) {
            return false;
        }

        Instant oldestDeletableTime = calculateOldestDeletableTime();
        return deletedAt.isBefore(oldestDeletableTime);
    }

    /**
     * validates the EFS mount.
     */
    private void validateEfsMount() {
        if (!efsMount.startsWith(File.separator)) {
            throw new EFSMountFormatException("EFS Mount must start with a " + File.separator);
        }

        if (efsMount.length() < 5) {
            throw new EFSMountFormatException("EFS mount must be at least 5 characters");
        }

        for (String directory : DISALLOWED_DIRECTORIES) {
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
        var jobUuid = getJobFromFile(path);
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
            // Actually log here because if we've gotten here without an exception, we deleted it.
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

        if (job == null) {
            FileTime creationTime = (FileTime) Files.getAttribute(path, "creationTime");
            return creationTime.toInstant();
        }

        if (!job.getStatus().isFinished()) {
            return null;
        }

        JobStatus status = job.getStatus();

        // If job status is cancelled then force deletion of folder immediately
        // because any data present will be removed
        if (status == JobStatus.CANCELLED || status == JobStatus.FAILED) {
            return Instant.now().minus(2L * auditFilesTTLHours, ChronoUnit.HOURS);
        }

        return job.getCompletedAt().toInstant();
    }

    private void logFileNotEligibleForDeletion(Path path) {
        log.info("File not eligible for deletion {}", path);
    }

    private void logFolderNotEligibleForDeletion(Path path) {
        log.info("File not eligible for deletion {}", path);
    }
}
