package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.util.fhir.FhirUtils;
import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.eventlogger.LogManager;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifier;
import static gov.cms.ab2d.worker.processor.ContractProcessorImpl.ID_EXT;
import static gov.cms.ab2d.worker.processor.coverage.CoverageMappingCallable.MBI_ID;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AddExtensionTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PatientClaimsProcessor patientClaimsProcessor;

    @Mock
    private LogManager eventLogger;

    private ContractProcessorImpl cut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        cut = new ContractProcessorImpl(jobRepository,
                patientClaimsProcessor, eventLogger);
    }

    @Test
    void testAddMbiIdToEobs() {
        String beneId = "1234";
        String mbi = "4567";

        org.hl7.fhir.dstu3.model.ExplanationOfBenefit b = new org.hl7.fhir.dstu3.model.ExplanationOfBenefit();
        String patientId = "Patient/" + beneId;
        org.hl7.fhir.dstu3.model.Reference ref = new org.hl7.fhir.dstu3.model.Reference();
        ref.setReference(patientId);
        b.setPatient(ref);
        List<IBaseResource> eobs = List.of(b);
        FilterOutByDate.DateRange dateRange = FilterOutByDate.getDateRange(1, 1900, 12,
                Calendar.getInstance().get(Calendar.YEAR));

        CoverageSummary summary = new CoverageSummary(createIdentifier(beneId, mbi), null, List.of(dateRange));

        Map<String, CoverageSummary> patients = new HashMap<>();
        patients.put(beneId, summary);

        FhirUtils.addMbiIdsToEobs(eobs, patients, STU3);

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

    @DisplayName("Add multiple mbis to a ")
    @Test
    void testAddMbiIdsToEobs() {
        String beneId = "1234";
        String mbi1 = "456";
        String mbi2 = "789";

        org.hl7.fhir.dstu3.model.ExplanationOfBenefit b = new org.hl7.fhir.dstu3.model.ExplanationOfBenefit();
        String patientId = "Patient/" + beneId;
        org.hl7.fhir.dstu3.model.Reference ref = new org.hl7.fhir.dstu3.model.Reference();
        ref.setReference(patientId);
        b.setPatient(ref);
        List<IBaseResource> eobs = List.of(b);
        FilterOutByDate.DateRange dateRange = FilterOutByDate.getDateRange(1, 1900, 12,
                Calendar.getInstance().get(Calendar.YEAR));

        CoverageSummary summary = new CoverageSummary(createIdentifier(beneId, mbi1, mbi2), null, List.of(dateRange));

        Map<String, CoverageSummary> patients = new HashMap<>();
        patients.put(beneId, summary);

        FhirUtils.addMbiIdsToEobs(eobs, patients, STU3);

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
