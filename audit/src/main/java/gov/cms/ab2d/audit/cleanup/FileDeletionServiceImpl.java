package gov.cms.ab2d.audit.cleanup;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.service.JobService;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import gov.cms.ab2d.common.util.EventUtils;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventSummary;
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
import java.util.Optional;
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
    private final LoggerEventSummary loggerEventSummary;

    private static final String FILE_EXTENSION = ".ndjson";

    private static final Set<String> DISALLOWED_DIRECTORIES = Set.of("/bin", "/boot", "/dev", "/etc", "/home", "/lib",
            "/opt", "/root", "/sbin", "/sys", "/usr", "/Applications", "/Library", "/Network", "/System", "/Users", "/Volumes");

    public FileDeletionServiceImpl(JobService jobService, LogManager eventLogger, LoggerEventSummary loggerEventSummary) {
        this.jobService = jobService;
        this.eventLogger = eventLogger;
        this.loggerEventSummary = loggerEventSummary;
    }

    /**
     * Delete all files that are in the efs mount with the .ndjson extension that are older than the TTL variable
     */
    @Override
    public void deleteFiles() {
        log.info("File deletion service kicked off");
        validateEfsMount();

        File[] files = new File(efsMount).listFiles();

        if (files == null || files.length == 0) {
            return;
        }

        List<String> jobIds = Stream.of(files).map(f -> f.getName()).collect(Collectors.toList());

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

            deleteExpiredJobFiles(jobIds, validFiles);
            deleteEmptyDirectories(jobIds, directories);

        } catch (IOException e) {
            log.error("Encountered exception while trying to gather the list of files to delete", e);
        }
    }

    /**
     * Delete any directories that are empty and expired.
     *
     * @param candidateDirs - the directories to delete (if they're empty and expired)
     */
    void deleteEmptyDirectories(List<String> jobIds, List<Path> candidateDirs) {
            for (Path directory : candidateDirs) {
            try {
                Job job = getJob(jobIds, directory);
                if (isEmptyDirectory(directory) && job != null && isExpiredDirectory(job, directory)) {
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

    void deleteExpiredJobFiles(List<String> jobIds, List<Path> validFiles) {
        // The list of jobs that were expired
        Set<String> jobsDeleted = new HashSet<>();
        for (Path file : validFiles) {

            try {
                // Get the JobId from the name
                var job = getJob(jobIds, file);
                var currentFileAge = getPathAge(file, job);
                final Instant deleteBoundary = calculateOldestDeletableTime();

                if (currentFileAge.isBefore(deleteBoundary) && matchesFilenameExtension(file)) {
                    deleteFile(file, job);

                    // If a job file was deleted make sure we capture that
                    if (job != null) {
                        jobsDeleted.add(job.getJobUuid());
                    }
                } else {
                    logFileNotEligibleForDeletion(file);
                }
            } catch (IOException io) {
                log.error("Encountered exception trying to delete a file {}, moving onto next one", file, io);
            } catch (Exception exception) {
                log.error("failed to delete or process a regular file {}", file.toUri().toString(), exception);
            }
        }
        for (String job : jobsDeleted) {
            eventLogger.log(LogManager.LogType.KINESIS, loggerEventSummary.getSummary(job));
        }
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

    private Job getJob(List<String> jobIds, Path path) {
        if (jobIds == null || jobIds.isEmpty()) {
            return null;
        }

        Optional<String> foundJob = jobIds.stream().filter(jobId -> path.toFile().getAbsolutePath().contains(jobId)).findAny();
        if (foundJob.isEmpty()) {
            return null;
        }

        String jobId = foundJob.get();
        return findJob(jobId, path);
    }

    /**
     * Check whether a directory is associated with a job, if the directory is check that the job has completed more
     * than x hours ago.
     * @param directory directory to check
     * @return true if directory belongs to a job that has finished long enough ago
     * @throws IOException should not throw ever because getDeleteCheckTime does not hit edge case
     */
    private boolean isExpiredDirectory(Job job, Path directory) throws IOException {
        // The only way getPathAge throws an IOException is if the job is null,
        // because that is never the case getPathAge should never throw an IOException
        Instant currentAge = getPathAge(directory, job);

        Instant deleteBoundary = calculateOldestDeletableTime();
        return currentAge.isBefore(deleteBoundary);
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

    private void deleteFile(Path path, Job job) throws IOException {
        FileEvent fileEvent = EventUtils.getFileEvent(job, new File(path.toUri()), FileEvent.FileStatus.DELETE);

        Files.delete(path);
        log.info("Deleted file {}", path);

        // If we reach this point then file was deleted without an exception so log it to Kinesis and SQL
        eventLogger.log(fileEvent);
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
    private boolean matchesFilenameExtension(Path path) {
        return path.toString().endsWith(FILE_EXTENSION.toLowerCase());
    }

    /**
     * File age is dictated by the answer to three questions: Is the file tied to a specific job? If so,
     * is the job a file is associated with complete? If so, when did it complete?
     *
     * 1. If the file is not tied to a specific job then just pull the creation time of the file.
     * 2. If the file is tied to a job then the file age is tied to the job age
     *      1. If the job is running the file is considered brand new
     *      2. If the job failed or was cancelled it is considered old and needs deletion
     *      3. If the job succeeded then the age of the file is considered the time the job completed
     * @param path path representing file
     * @param job job, if relevant, that path belongs to
     * @return age of the file as an instant
     * @throws IOException on failure to get creation time of file, only thrown when no job is associated with the file
     */
    private Instant getPathAge(Path path, Job job)  throws IOException {

        if (job == null) {
            FileTime creationTime = (FileTime) Files.getAttribute(path, "creationTime");
            return creationTime.toInstant();
        }

        // If job is currently running ignore this file for now
        if (!job.getStatus().isFinished()) {
            return Instant.now();
        }

        JobStatus status = job.getStatus();

        // If job status is cancelled or failed then force deletion of folder immediately
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
