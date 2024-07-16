package gov.cms.ab2d.worker.util;

import gov.cms.ab2d.coverage.model.Identifiers;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.filter.FilterOutByDate;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Extension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static gov.cms.ab2d.fhir.ExtensionUtils.ID_EXT;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.fhir.PatientIdentifier.MBI_ID;
import static org.junit.jupiter.api.Assertions.*;

public class FhirUtilsTest {

    @DisplayName("Add mbi to an eob works and adds in expected format")
    @Test
    void testAddMbiIdToEobs() {
        Long beneId = 1234L;
        String mbi = "4567";

        org.hl7.fhir.dstu3.model.ExplanationOfBenefit b = new org.hl7.fhir.dstu3.model.ExplanationOfBenefit();
        String patientId = "Patient/" + beneId;
        org.hl7.fhir.dstu3.model.Reference ref = new org.hl7.fhir.dstu3.model.Reference();
        ref.setReference(patientId);
        b.setPatient(ref);
        FilterOutByDate.DateRange dateRange = FilterOutByDate.getDateRange(1, 1900, 12,
                Calendar.getInstance().get(Calendar.YEAR));

        CoverageSummary summary = new CoverageSummary(createIdentifier(beneId, mbi), null, List.of(dateRange));
        FhirUtils.addMbiIdsToEobs(b, summary, STU3);

        List<org.hl7.fhir.dstu3.model.Extension> extensions = b.getExtension();

        assertNotNull(extensions);
        assertEquals(1, extensions.size());
        org.hl7.fhir.dstu3.model.Extension ext = extensions.get(0);
        assertEquals(ID_EXT, ext.getUrl());

        org.hl7.fhir.dstu3.model.Identifier id = (org.hl7.fhir.dstu3.model.Identifier) ext.getValue();
        assertNotNull(id);
        assertEquals(MBI_ID, id.getSystem());
        assertEquals(mbi, id.getValue());
    }

    @DisplayName("Add null mbi to eob as null")
    @Test
    void testAddNullMbiIdToEobs() {
        Long beneId = 1234L;

        org.hl7.fhir.dstu3.model.ExplanationOfBenefit b = new org.hl7.fhir.dstu3.model.ExplanationOfBenefit();
        String patientId = "Patient/" + beneId;
        org.hl7.fhir.dstu3.model.Reference ref = new org.hl7.fhir.dstu3.model.Reference();
        ref.setReference(patientId);
        b.setPatient(ref);
        FilterOutByDate.DateRange dateRange = FilterOutByDate.getDateRange(1, 1900, 12,
                Calendar.getInstance().get(Calendar.YEAR));

        CoverageSummary summary = new CoverageSummary(createIdentifier(beneId, null, "HIST_MBI"), null, List.of(dateRange));

        FhirUtils.addMbiIdsToEobs(b, summary, STU3);

        List<org.hl7.fhir.dstu3.model.Extension> extensions = b.getExtension();

        assertNotNull(extensions);
        assertEquals(1, extensions.size());
        org.hl7.fhir.dstu3.model.Extension ext = extensions.get(0);
        assertEquals(ID_EXT, ext.getUrl());

        org.hl7.fhir.dstu3.model.Identifier id = (org.hl7.fhir.dstu3.model.Identifier) ext.getValue();
        assertNotNull(id);
        assertEquals(MBI_ID, id.getSystem());
        assertEquals("HIST_MBI", id.getValue());
        assertEquals(1, id.getExtension().size());
        Extension idExt = id.getExtension().get(0);
        Coding c = (Coding) idExt.getValue();
        assertEquals("historic", c.getCode());
    }

    @DisplayName("Process missing mbi but do not fail")
    @Test
    void testAddNoMbiIdToEobs() {
        Long beneId = 1234L;

        org.hl7.fhir.dstu3.model.ExplanationOfBenefit b = new org.hl7.fhir.dstu3.model.ExplanationOfBenefit();
        String patientId = "Patient/" + beneId;
        org.hl7.fhir.dstu3.model.Reference ref = new org.hl7.fhir.dstu3.model.Reference();
        ref.setReference(patientId);
        b.setPatient(ref);
        FilterOutByDate.DateRange dateRange = FilterOutByDate.getDateRange(1, 1900, 12,
                Calendar.getInstance().get(Calendar.YEAR));

        CoverageSummary summary = new CoverageSummary(createIdentifier(beneId, null), null, List.of(dateRange));

        FhirUtils.addMbiIdsToEobs(b, summary, STU3);

        List<org.hl7.fhir.dstu3.model.Extension> extensions = b.getExtension();

        assertNotNull(extensions);
        assertEquals(0, extensions.size());
    }

    @DisplayName("Add multiple mbis to an eob")
    @Test
    void testAddMbiIdsToEobs() {
        Long beneId = 1234L;
        String mbi1 = "456";
        String mbi2 = "789";

        org.hl7.fhir.dstu3.model.ExplanationOfBenefit b = new org.hl7.fhir.dstu3.model.ExplanationOfBenefit();
        String patientId = "Patient/" + beneId;
        org.hl7.fhir.dstu3.model.Reference ref = new org.hl7.fhir.dstu3.model.Reference();
        ref.setReference(patientId);
        b.setPatient(ref);
        FilterOutByDate.DateRange dateRange = FilterOutByDate.getDateRange(1, 1900, 12,
                Calendar.getInstance().get(Calendar.YEAR));

        CoverageSummary summary = new CoverageSummary(createIdentifier(beneId, mbi1, mbi2), null, List.of(dateRange));

        FhirUtils.addMbiIdsToEobs(b, summary, STU3);

        List<org.hl7.fhir.dstu3.model.Extension> extensions = b.getExtension();

        assertNotNull(extensions);
        assertEquals(2, extensions.size());

        for (org.hl7.fhir.dstu3.model.Extension ext : extensions) {
            assertEquals(ID_EXT, ext.getUrl());

            org.hl7.fhir.dstu3.model.Identifier id = (org.hl7.fhir.dstu3.model.Identifier) ext.getValue();
            assertNotNull(id);
            assertFalse(id.isEmpty());

            assertTrue(id.getValue().equals("456") || id.getValue().equals("789"));
        }
    }

    @Test
    void testNullInputs() {
        assertDoesNotThrow(() -> {
            FhirUtils.addMbiIdsToEobs(null, null, null);
        });
    }

    public static Identifiers createIdentifier(long beneficiaryId, String currentMbi, String... historicMbis) {
        return new Identifiers(beneficiaryId, currentMbi, new LinkedHashSet<>(Set.of(historicMbis)));
    }

}
