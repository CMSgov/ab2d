package gov.cms.ab2d.common.util;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.fhir.Versions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JobUtilTest {

    @Test
    void isJobDone() {
        assertFalse(JobUtil.isJobDone(null));
        assertFalse(JobUtil.isJobDone(new Job()));
        Job job = createBasicJob(JobStatus.CANCELLED, null, false);
        assertTrue(JobUtil.isJobDone(job));
        job = createBasicJob(JobStatus.CANCELLED, null, true);
        assertTrue(JobUtil.isJobDone(job));
        job = createBasicJob(JobStatus.FAILED, null, false);
        assertTrue(JobUtil.isJobDone(job));
        job = createBasicJob(JobStatus.FAILED, null, true);
        assertTrue(JobUtil.isJobDone(job));
        job = createBasicJob(JobStatus.IN_PROGRESS, new boolean[] {true, true}, true);
        assertFalse(JobUtil.isJobDone(job));
        job = createBasicJob(JobStatus.SUBMITTED, new boolean[] {true, true}, true);
        assertFalse(JobUtil.isJobDone(job));
        job = createBasicJob(JobStatus.SUCCESSFUL, new boolean[] {true, true}, false);
        assertTrue(JobUtil.isJobDone(job));
        job = createBasicJob(JobStatus.SUCCESSFUL, new boolean[] {false, true}, false);
        assertFalse(JobUtil.isJobDone(job));
        job = createBasicJob(JobStatus.SUCCESSFUL, new boolean[] {false, true}, true);
        assertTrue(JobUtil.isJobDone(job));
        job = createBasicJob(JobStatus.SUCCESSFUL, new boolean[] {true, true}, true);
        assertTrue(JobUtil.isJobDone(job));
        job = createBasicJob(JobStatus.SUCCESSFUL, null, true);
        assertTrue(JobUtil.isJobDone(job));
        job = createBasicJob(JobStatus.SUCCESSFUL, null, false);
        assertFalse(JobUtil.isJobDone(job));
        job = createBasicJob(JobStatus.SUCCESSFUL, new boolean[] {false, true}, false);
        JobOutput error = job.getJobOutputs().stream().filter(c -> c.getDownloaded() == false).findFirst().orElse(null);
        error.setError(true);
        assertTrue(JobUtil.isJobDone(job));
    }

    private Job createBasicJob(JobStatus status, boolean[] outputsDownloaded, boolean isExpired) {
        Job job = new Job();
        job.setId(1L);
        job.setJobUuid("JOB");
        job.setStatus(status);
        job.setFhirVersion(Versions.FhirVersions.STU3);
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