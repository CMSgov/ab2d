package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Covers the error/skip behavior of the prototype, making sure that the job fails when it hits its failure
 * threshold, that failing benes are skipped, and that failures are written to the error file.
 */
class PrototypeErrorAndSkipIntegrationTest extends AbstractPrototypeRecoveryIntegrationTest {

    @Test
    @DisplayName("A resource that fails to serialize is written to the _error output but the job still succeeds")
    void serializationFailureIsWrittenToErrorOutput() throws Exception {
        Job job = createSubmittedV3Job("err-output");
        String uuid = job.getJobUuid();
        long badBene = 6L;

        // Return an un-encodable resource for one bene only
        doAnswer(inv -> {
            CoverageSummary patient = inv.getArgument(1);
            long id = patientId(patient);
            processedLog.add(id);
            return id == badBene ? List.of(mock(IBaseResource.class)) : List.of(eobFor(id));
        }).when(patientClaimsProcessor).getEobBundleResources(any(), any());

        Job result = prototypeJobProcessor.process(uuid);
        assertEquals(JobStatus.SUCCESSFUL, result.getStatus(), "a serialization error should not fail the job");

        // an error output file exists, is a valid OperationOutcome, and is registered as an error JobOutput
        List<Path> errorFiles = deliveredErrorFiles(uuid);
        assertFalse(errorFiles.isEmpty(), "expected an _error.ndjson.gz output for the serialization failure");
        assertTrue(linesOf(errorFiles.get(0)).stream().anyMatch(line -> line.contains("OperationOutcome")),
                "the error output should contain an OperationOutcome");
        assertTrue(jobOutputFilePaths(uuid).stream().anyMatch(p -> p.endsWith("_error.ndjson.gz")),
                "an error JobOutput row should be registered: " + jobOutputFilePaths(uuid));

        // the good benes are still delivered in the output and the un-encodable one doesn't appear
        Set<Long> data = new HashSet<>(benesIn(deliveredOutputFiles(uuid)));
        assertFalse(data.contains(badBene), "the un-encodable bene should not appear in the data output");
        assertEquals(TOTAL_BENES - 1, data.size(), "every other beneficiary should be in the data output");
    }

    @Test
    @DisplayName("A persistently-failing beneficiary is skipped and the job still succeeds under the threshold")
    void aPersistentlyFailingBeneIsSkipped() throws Exception {
        Job job = createSubmittedV3Job("skip-under");
        String uuid = job.getJobUuid();
        long failBene = 6L;

        // A persistent, non-transient failure is skipped (not retried) and counted as one error
        doAnswer(inv -> {
            CoverageSummary patient = inv.getArgument(1);
            long id = patientId(patient);
            if (id == failBene) {
                throw new IllegalStateException("persistent failure for bene " + failBene);
            }
            processedLog.add(id);
            return List.of(eobFor(id));
        }).when(patientClaimsProcessor).getEobBundleResources(any(), any());

        Job result = prototypeJobProcessor.process(uuid);
        assertEquals(JobStatus.SUCCESSFUL, result.getStatus(), "one skipped bene is under the failure threshold");

        Set<Long> data = new HashSet<>(benesIn(deliveredOutputFiles(uuid)));
        assertFalse(data.contains(failBene), "the skipped bene must not be in the output");
        assertEquals(TOTAL_BENES - 1, data.size(), "every other beneficiary should be delivered exactly once");
    }

    @Test
    @DisplayName("Too many failing beneficiaries trips the failure threshold and fails the job")
    void tooManyFailingBenesFailsTheJob() throws Exception {
        Job job = createSubmittedV3Job("skip-over");
        String uuid = job.getJobUuid();
        Set<Long> failBenes = Set.of(6L, 14L);

        // Two failures out of 20 reaches the 10% threshold, so the job fails terminally.
        doAnswer(inv -> {
            CoverageSummary patient = inv.getArgument(1);
            long id = patientId(patient);
            if (failBenes.contains(id)) {
                throw new IllegalStateException("persistent failure for bene " + id);
            }
            processedLog.add(id);
            return List.of(eobFor(id));
        }).when(patientClaimsProcessor).getEobBundleResources(any(), any());

        Job result = prototypeJobProcessor.process(uuid);
        assertEquals(JobStatus.FAILED, result.getStatus(),
                "reaching the failure threshold should fail the job terminally");
    }

    /** A minimal eob referencing the given id. */
    private IBaseResource eobFor(long id) {
        org.hl7.fhir.r4.model.ExplanationOfBenefit eob = new org.hl7.fhir.r4.model.ExplanationOfBenefit();
        eob.setId("eob-" + id);
        eob.getPatient().setReference("Patient/" + id);
        return eob;
    }
}
