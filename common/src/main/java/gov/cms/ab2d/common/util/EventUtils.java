package gov.cms.ab2d.common.util;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.eventlogger.events.JobStatusChangeEvent;

import java.io.File;

public class EventUtils {
    public static JobStatusChangeEvent getJobChangeEvent(Job job, JobStatus jobStatus, String message) {
        return new JobStatusChangeEvent(getClientId(job), getJobId(job), getJobStatus(job),
                jobStatus == null ? null : jobStatus.name(),
                message);
    }

    public static FileEvent getFileEvent(Job job, File file, FileEvent.FileStatus status) {
        return new FileEvent(getClientId(job), getJobId(job), file, status);
    }

    private static String getClientId(Job job) {
        return job != null && job.getPdpClient() != null ? job.getPdpClient().getClientId() : null;
    }

    private static String getJobId(Job job) {
        return job == null ? null : job.getJobUuid();
    }

    private static String getJobStatus(Job job) {
        if (job == null || job.getStatus() == null) {
            return null;
        }
        return job.getStatus().name();
    }
}