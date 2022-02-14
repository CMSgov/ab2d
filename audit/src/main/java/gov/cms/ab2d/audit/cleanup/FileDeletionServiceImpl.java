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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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

        List<String> jobIds = Stream.of(files).map(File::getName).toList();
        for (String jobId : jobIds) {
            Job job = findJob(jobId);
            if (job == null) {
                log.trace("No job connected to id {}", jobId); // Put a log statement here to make PMD happy
                continue;
            }
            boolean jobExpired = shouldJobFilesBeDeleted(job);
            if (jobExpired) {
                deleteJobDirectory(job);
            }
        }
    }

    void deleteJobDirectory(Job job) {
        Path jobTopLevelDir = Path.of(efsMount, job.getJobUuid());
        deleteNdjsonFilesAndDirectory(job, jobTopLevelDir);
        try {
            if (isEmptyDirectory(jobTopLevelDir)) {
                Files.deleteIfExists(jobTopLevelDir);
                log.info("Deleted top level job directory {}", jobTopLevelDir.toFile().getAbsolutePath());
            }
        } catch (Exception ex) {
            log.error("Unable to delete top level job {} directory", jobTopLevelDir);
        }
    }

    /**
     * Recursively delete NDJON files and sub directories
     *
     * @param job - the job
     * @param jobDir - the top level directory
     */
    void deleteNdjsonFilesAndDirectory(Job job, Path jobDir) {
        for (File file : jobDir.toFile().listFiles()) {
            Path filePath = Path.of(file.getAbsolutePath());
            if (file.exists() && Files.isRegularFile(filePath) && matchesFilenameExtension(filePath)) {
                try {
                    deleteFile(filePath, job);
                } catch (Exception ex) {
                    log.error("Unable to delete file " + file.getAbsolutePath(), ex);
                }
            } else if (file.isDirectory()) {
                deleteNdjsonFilesAndDirectory(job, filePath);
                try (Stream<Path> children =  Files.list(filePath)) {
                    if (children.findAny().isEmpty()) {
                        Files.deleteIfExists(filePath);
                        log.info("Deleted directory {}", filePath);
                    } else {
                        logFolderNotEligibleForDeletion(filePath);
                    }
                } catch (Exception ex) {
                    log.error("Unable to list files in directory" + file.getAbsolutePath(), ex);
                }
            } else {
                logFileNotEligibleForDeletion(filePath);
            }
        }
    }

    boolean shouldJobFilesBeDeleted(Job job) {
        // If job is currently running ignore this file for now
        if (!job.getStatus().isFinished()) {
            return false;
        }

        if (job.getStatus() == JobStatus.CANCELLED || job.getStatus() == JobStatus.FAILED) {
            return true;
        }

        // If it is completed, check the age
        OffsetDateTime completedTime = job.getCompletedAt();
        if (completedTime == null) {
            return false;
        }

        final Instant deleteBoundary = calculateOldestDeletableTime();
        return completedTime.toInstant().isBefore(deleteBoundary);
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

        if (path.toFile().exists()) {
            Files.delete(path);
            log.info("Deleted file {}", path);
        }

        // If we reach this point then file was deleted without an exception so log it to Kinesis and SQL
        eventLogger.log(fileEvent);
    }

    /**
     * Given a jobUuid, finds the job
     *
     * @param jobUuid - the job id
     * @return the Job object
     */
    private Job findJob(String jobUuid) {
        try {
            return jobService.getJobByJobUuid(jobUuid);
        } catch (ResourceNotFoundException ex) {
            return null;
        }
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

    private void logFileNotEligibleForDeletion(Path path) {
        log.info("File not eligible for deletion {}", path);
    }

    private void logFolderNotEligibleForDeletion(Path path) {
        log.info("Folder not eligible for deletion {}", path);
    }
}
