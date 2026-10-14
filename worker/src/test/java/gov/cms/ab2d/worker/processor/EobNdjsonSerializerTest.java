package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.fhir.FhirVersion;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the eob serializer.
 */
class EobNdjsonSerializerTest {

    private final FhirVersion version = FhirVersion.R4V3;

    @Test
    @DisplayName("every resource that encodes cleanly becomes a data line, with no error lines")
    void allResourcesSerializeToDataLines() {
        SerializedEobs result = EobNdjsonSerializer.serialize(version, List.of(eob(1), eob(2)));

        assertEquals(2, result.dataLines().size());
        assertTrue(result.errorLines().isEmpty(), "no resource failed, so there should be no error lines");
        assertTrue(result.dataLines().get(0).contains("Patient/1"));
        assertTrue(result.dataLines().get(1).contains("Patient/2"));
    }

    @Test
    @DisplayName("a resource that fails to encode becomes an OperationOutcome error line, others still succeed")
    void aFailingResourceBecomesAnOperationOutcome() {
        // mock of the resource will force an encoding error
        IBaseResource unencodable = mock(IBaseResource.class);

        SerializedEobs result = EobNdjsonSerializer.serialize(version, List.of(eob(1), unencodable));

        assertEquals(1, result.dataLines().size(), "the encodable resource is still written");
        assertTrue(result.dataLines().get(0).contains("Patient/1"));
        assertEquals(1, result.errorLines().size(), "the unencodable resource becomes an error line");
        assertTrue(result.errorLines().get(0).contains("OperationOutcome"),
                "the error line should be an OperationOutcome: " + result.errorLines().get(0));
    }

    @Test
    @DisplayName("null or empty input yields an empty result")
    void nullOrEmptyYieldsEmpty() {
        assertTrue(EobNdjsonSerializer.serialize(version, null).isEmpty());
        assertTrue(EobNdjsonSerializer.serialize(version, List.of()).isEmpty());
    }

    private IBaseResource eob(long id) {
        ExplanationOfBenefit eob = new ExplanationOfBenefit();
        eob.setId("eob-" + id);
        eob.getPatient().setReference("Patient/" + id);
        return eob;
    }
}
