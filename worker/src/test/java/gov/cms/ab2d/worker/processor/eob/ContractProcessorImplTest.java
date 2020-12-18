package gov.cms.ab2d.worker.processor.eob;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.common.model.Identifiers;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.processor.eob.ContractProcessorImpl;
import gov.cms.ab2d.worker.processor.eob.PatientClaimsProcessor;
import gov.cms.ab2d.worker.service.FileService;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.*;

import static gov.cms.ab2d.worker.processor.eob.ContractProcessorImpl.ID_EXT;
import static gov.cms.ab2d.worker.processor.eob.PatientContractCallable.*;
import static gov.cms.ab2d.worker.processor.eob.PatientContractCallable.MBI_ID;
import static org.junit.jupiter.api.Assertions.*;

class ContractProcessorImplTest {

    @Mock
    private FileService fileService;

    @Mock
    private PatientClaimsProcessor patientClaimsProcessor;

    @Mock
    private LogManager eventLogger;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private FhirContext fhirContext;

    private ContractProcessorImpl processor;

    @BeforeEach
    public void before() {
        processor = new ContractProcessorImpl(fileService, jobRepository, patientClaimsProcessor,
                eventLogger, fhirContext);


    }

    @Test
    @DisplayName("Creating mbis is done successfully")
    void createMbiExtensions() {
        LinkedHashSet<String> historic = new LinkedHashSet<>();
        historic.add("historic-mbi-1");
        historic.add("historic-mbi-2");

        Identifiers identifiers = new Identifiers("bene-id", "current-mbi", historic);

        ExplanationOfBenefit eob = new ExplanationOfBenefit();
        eob.setPatient(new Reference().setReference("Patient/bene-id"));

        Map<String, ContractBeneficiaries.PatientDTO> patientDTOs = new HashMap<>();

        ContractBeneficiaries.PatientDTO dto = new ContractBeneficiaries.PatientDTO();
        dto.setIdentifiers(identifiers);

        patientDTOs.put("bene-id", dto);

        processor.addMbiIdsToEobs(Collections.singletonList(eob), patientDTOs);

        assertFalse(eob.getExtension().isEmpty());
        assertEquals(3, eob.getExtension().size());

        List<Extension> extensions = eob.getExtension();

        // Check that each extension is an id extension
        extensions.forEach(extension -> {
            assertEquals(ID_EXT, extension.getUrl());
            assertNotNull(extension.getValue());
        });

        checkCurrentMbi(extensions.get(0));

        List<Extension> historicMbis = extensions.subList(1, extensions.size());
        checkHistoricalMbi(historicMbis.get(0), "historic-mbi-1");
        checkHistoricalMbi(historicMbis.get(1), "historic-mbi-2");

    }

    private void checkCurrentMbi(Extension currentMbi) {
        // Check that current mbi has correct format
        Identifier currentId = (Identifier) currentMbi.getValue();
        assertEquals(MBI_ID, currentId.getSystem());
        assertEquals("current-mbi", currentId.getValue());

        assertFalse(currentId.getExtension().isEmpty());
        Extension currentExtension = currentId.getExtension().get(0);
        assertEquals(CURRENCY_IDENTIFIER, currentExtension.getUrl());
        assertEquals(CURRENT_MBI, ((Coding) currentExtension.getValue()).getCode());
    }

    private void checkHistoricalMbi(Extension historicalExtension, String id) {
        Identifier historicalId = (Identifier) historicalExtension.getValue();
        assertEquals(MBI_ID, historicalId.getSystem());
        assertEquals(id, historicalId.getValue());

        assertFalse(historicalId.getExtension().isEmpty());
        Extension historicExtension = historicalId.getExtension().get(0);
        assertEquals(CURRENCY_IDENTIFIER, historicExtension.getUrl());
        assertEquals(HISTORIC_MBI, ((Coding) historicExtension.getValue()).getCode());
    }
}
