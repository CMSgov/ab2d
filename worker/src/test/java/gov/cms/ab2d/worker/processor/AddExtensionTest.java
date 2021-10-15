package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.common.util.fhir.FhirUtils;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Extension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.List;

import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifier;
import static gov.cms.ab2d.worker.processor.ContractProcessorImpl.ID_EXT;
import static gov.cms.ab2d.worker.processor.coverage.CoverageMappingCallable.MBI_ID;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AddExtensionTest {

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

    @DisplayName("Add multiple mbis to a ")
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
}
