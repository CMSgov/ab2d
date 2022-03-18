package gov.cms.ab2d.common.util;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.JobStatus;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
public class JobUtil {

    /**
     * A job is done if the status is either CANCELLED or FAILED
     * If a job status is SUCCESSFUL, it is done if all files have been downloaded or they have expired
     *
     * @param job
     * @return
     */
    public static boolean isJobDone(Job job) {
        try {
            // Job is still in progress
            if (job == null || job.getStatus() == null || job.getStatus() == JobStatus.IN_PROGRESS || job.getStatus() == JobStatus.SUBMITTED) {
                return false;
            }

            // Job has finished but was not successful
            if (job.getStatus() == JobStatus.CANCELLED || job.getStatus() == JobStatus.FAILED) {
                return true;
            }

            // Job was successful - now did the client download the files or is it expired

            // Job has expired, it's done.
            if (job.getExpiresAt() != null && job.getExpiresAt().isBefore(OffsetDateTime.now())) {
                return true;
            }

            // If it hasn't expired, look to see if all files have been downloaded, if so, it's done
            List<JobOutput> jobOutputs = job.getJobOutputs();
            if (jobOutputs == null || jobOutputs.isEmpty()) {
                return false;
            }
            JobOutput aRemaining = jobOutputs.stream()
                    .filter(c -> c.getError() == null || !c.getError())
                    .filter(c -> c.getDownloaded()==0).findAny().orElse(null);

            return aRemaining == null;
        } catch (Exception ex) {
            // Logging should never break anything
            String jobId = job.getJobUuid();
            log.error("Unable to determine if job " + jobId + " is done", ex);
            return false;
        }
    }
}
