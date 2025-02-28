package gov.cms.ab2d.job.model;

import gov.cms.ab2d.common.model.TooFrequentInvocations;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobTest {

    @Test
    void pollingCheck() {
        Job job = new Job();
        // First time - ok
        job.pollAndUpdateTime(1);
        // Too soon, expect the exception
        assertThrows(TooFrequentInvocations.class, () -> job.pollAndUpdateTime(1));
    }

    // Silliness to improve the code coverage number.
    @Test
    void codeCoverageGetterSetter() {
        Job job = new Job();
        //noinspection ResultOfMethodCallIgnored
        job.hashCode();
        //noinspection ResultOfMethodCallIgnored,EqualsWithItself
        job.equals(job);
        job.addJobOutput(new JobOutput());
        job.setSince(job.getSince());
        job.setUntil(job.getUntil());
        job.setSinceSource(job.getSinceSource());
        assertFalse(job.hasJobBeenCancelled());
        job.setStatus(JobStatus.CANCELLED);
        assertTrue(job.hasJobBeenCancelled());
    }
}
