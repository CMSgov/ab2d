package gov.cms.ab2d.worker.processor.eob;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import org.hl7.fhir.dstu3.model.*;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.worker.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static gov.cms.ab2d.worker.processor.eob.BundleUtils.createIdentifier;
import static gov.cms.ab2d.worker.processor.eob.ContractProcessorImpl.ID_EXT;
import static gov.cms.ab2d.worker.processor.coverage.CoverageMappingCallable.MBI_ID;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AddExtensionTest {
    @Mock
    private FileService fileService;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PatientClaimsProcessor patientClaimsProcessor;

    @Mock
    private LogManager eventLogger;

    @Mock
    private FhirContext fhirContext;

    private ContractProcessorImpl cut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        cut = new ContractProcessorImpl(fileService, jobRepository,
                patientClaimsProcessor, eventLogger, fhirContext);
    }

    @Test
    void testAddMbiIdToEobs() {
        String beneId = "1234";
        String mbi = "4567";


        ExplanationOfBenefit b = new ExplanationOfBenefit();
        String patientId = "Patient/" + beneId;
        Reference ref = new Reference();
        ref.setReference(patientId);
        b.setPatient(ref);
        List<ExplanationOfBenefit> eobs = List.of(b);
        FilterOutByDate.DateRange dateRange = FilterOutByDate.getDateRange(1, 1900, 12,
                Calendar.getInstance().get(Calendar.YEAR));

        ContractBeneficiaries.PatientDTO patientDTO = new ContractBeneficiaries.PatientDTO(
                createIdentifier(beneId, mbi), List.of(dateRange));

        Map<String, ContractBeneficiaries.PatientDTO> patients = new HashMap<>();
        patients.put(beneId, patientDTO);

        cut.addMbiIdsToEobs(eobs, patients);

        List<Extension> extensions = b.getExtension();

        assertNotNull(extensions);
        assertEquals(1, extensions.size());
        Extension ext = extensions.get(0);
        assertEquals(ID_EXT, ext.getUrl());

        Identifier id = (Identifier) ext.getValue();
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

        ExplanationOfBenefit b = new ExplanationOfBenefit();
        String patientId = "Patient/" + beneId;
        Reference ref = new Reference();
        ref.setReference(patientId);
        b.setPatient(ref);
        List<ExplanationOfBenefit> eobs = List.of(b);
        FilterOutByDate.DateRange dateRange = FilterOutByDate.getDateRange(1, 1900, 12,
                Calendar.getInstance().get(Calendar.YEAR));

        ContractBeneficiaries.PatientDTO patientDTO = new ContractBeneficiaries.PatientDTO(
                createIdentifier(beneId, mbi1, mbi2), List.of(dateRange));

        Map<String, ContractBeneficiaries.PatientDTO> patients = new HashMap<>();
        patients.put(beneId, patientDTO);

        cut.addMbiIdsToEobs(eobs, patients);

        List<Extension> extensions = b.getExtension();

        assertNotNull(extensions);
        assertEquals(2, extensions.size());

        for (Extension ext : extensions) {
            assertEquals(ID_EXT, ext.getUrl());

            Identifier id = (Identifier) ext.getValue();
            assertNotNull(id);
            assertFalse(id.isEmpty());

            assertTrue(id.getValue().equals("456") || id.getValue().equals("789"));
        }
    }
}
