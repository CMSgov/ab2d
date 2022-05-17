package gov.cms.ab2d.job.util;

import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobOutput;
import gov.cms.ab2d.job.model.JobStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static gov.cms.ab2d.common.util.Constants.MAX_DOWNLOADS;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.*;

class JobUtilTest {
    @Test
    void isJobDone() {
        assertFalse(JobUtil.isJobDone(null, MAX_DOWNLOADS));
        assertFalse(JobUtil.isJobDone(new Job(), MAX_DOWNLOADS));
        Job job = createBasicJob(JobStatus.CANCELLED, null, false);
        assertTrue(JobUtil.isJobDone(job, MAX_DOWNLOADS));
        job = createBasicJob(JobStatus.CANCELLED, null, true);
        assertTrue(JobUtil.isJobDone(job, MAX_DOWNLOADS));
        job = createBasicJob(JobStatus.FAILED, null, false);
        assertTrue(JobUtil.isJobDone(job, MAX_DOWNLOADS));
        job = createBasicJob(JobStatus.FAILED, null, true);
        assertTrue(JobUtil.isJobDone(job, MAX_DOWNLOADS));
        job = createBasicJob(JobStatus.IN_PROGRESS, new int[]{1, 1}, true);
        assertFalse(JobUtil.isJobDone(job, MAX_DOWNLOADS));
        job = createBasicJob(JobStatus.SUBMITTED, new int[]{1, 1}, true);
        assertFalse(JobUtil.isJobDone(job, MAX_DOWNLOADS));
        job = createBasicJob(JobStatus.SUCCESSFUL, new int[]{MAX_DOWNLOADS, MAX_DOWNLOADS}, false);
        assertTrue(JobUtil.isJobDone(job, MAX_DOWNLOADS));
        job = createBasicJob(JobStatus.SUCCESSFUL, new int[]{0, 11}, false);
        assertFalse(JobUtil.isJobDone(job, MAX_DOWNLOADS));
        job = createBasicJob(JobStatus.SUCCESSFUL, new int[]{0, 1}, true);
        assertTrue(JobUtil.isJobDone(job, MAX_DOWNLOADS));
        job = createBasicJob(JobStatus.SUCCESSFUL, new int[]{1, 1}, true);
        assertTrue(JobUtil.isJobDone(job, MAX_DOWNLOADS));
        job = createBasicJob(JobStatus.SUCCESSFUL, null, true);
        assertTrue(JobUtil.isJobDone(job, MAX_DOWNLOADS));
        job = createBasicJob(JobStatus.SUCCESSFUL, null, false);
        assertFalse(JobUtil.isJobDone(job, MAX_DOWNLOADS));
        job = createBasicJob(JobStatus.SUCCESSFUL, new int[]{0, MAX_DOWNLOADS}, false);
        JobOutput error = job.getJobOutputs().stream().filter(c -> c.getDownloaded() == 0).findFirst().orElse(null);
        error.setError(true);
        assertTrue(JobUtil.isJobDone(job, MAX_DOWNLOADS));
    }

    private Job createBasicJob(JobStatus status, int[] outputsDownloaded, boolean isExpired) {
        Job job = new Job();
        job.setId(1L);
        job.setJobUuid("JOB");
        job.setStatus(status);
        job.setFhirVersion(STU3);
        OffsetDateTime expiresAt = OffsetDateTime.now();
        if (isExpired) {
            expiresAt = expiresAt.minus(1, ChronoUnit.HOURS);
        } else {
            expiresAt = expiresAt.plus(1, ChronoUnit.HOURS);
        }
        job.setExpiresAt(expiresAt);
        List<JobOutput> jobOutputs = new ArrayList<>();
        job.setJobOutputs(jobOutputs);
        if (outputsDownloaded != null) {
            for (int i = 0; i < outputsDownloaded.length; i++) {
                JobOutput output = new JobOutput();
                output.setDownloaded(outputsDownloaded[i]);
                jobOutputs.add(output);
            }
        }
        return job;
    }

    @Test
    void testLists() {
        Set<String> vals = new HashSet<>();
        vals.add("Hello");
        vals.add("Hello");
        vals.add("World");
        assertEquals(2, vals.size());
    }
}