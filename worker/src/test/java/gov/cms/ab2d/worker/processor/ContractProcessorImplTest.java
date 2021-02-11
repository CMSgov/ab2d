package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.model.Identifiers;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.util.fhir.FhirUtils;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.fhir.Versions;
import gov.cms.ab2d.worker.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.*;

import static gov.cms.ab2d.worker.processor.ContractProcessorImpl.ID_EXT;
import static gov.cms.ab2d.worker.processor.coverage.CoverageMappingCallable.*;
import static java.util.Collections.singletonList;
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
        processor = new ContractProcessorImpl(fileService, jobRepository, patientClaimsProcessor, eventLogger);
    }

    @Test
    @DisplayName("Creating mbis is done successfully")
    void createMbiExtensions() {
        LinkedHashSet<String> historic = new LinkedHashSet<>();
        historic.add("historic-mbi-1");
        historic.add("historic-mbi-2");

        Identifiers identifiers = new Identifiers("bene-id", "current-mbi", historic);

        org.hl7.fhir.dstu3.model.ExplanationOfBenefit eob = new org.hl7.fhir.dstu3.model.ExplanationOfBenefit();
        eob.setPatient(new org.hl7.fhir.dstu3.model.Reference().setReference("Patient/bene-id"));

        Map<String, CoverageSummary> coverageSummaries = new HashMap<>() {{
                put(identifiers.getBeneficiaryId(), new CoverageSummary(identifiers, null, null));
        }};

        FhirUtils.addMbiIdsToEobs(singletonList(eob), coverageSummaries, Versions.FhirVersions.STU3);

        assertFalse(eob.getExtension().isEmpty());
        assertEquals(3, eob.getExtension().size());

        List<org.hl7.fhir.dstu3.model.Extension> extensions = eob.getExtension();

        // Check that each extension is an id extension
        extensions.forEach(extension -> {
            assertEquals(ID_EXT, extension.getUrl());
            assertNotNull(extension.getValue());
        });

        checkCurrentMbi(extensions.get(0));

        List<org.hl7.fhir.dstu3.model.Extension> historicMbis = extensions.subList(1, extensions.size());
        checkHistoricalMbi(historicMbis.get(0), "historic-mbi-1");
        checkHistoricalMbi(historicMbis.get(1), "historic-mbi-2");

    }

    private void checkCurrentMbi(org.hl7.fhir.dstu3.model.Extension currentMbi) {
        // Check that current mbi has correct format
        org.hl7.fhir.dstu3.model.Identifier currentId = (org.hl7.fhir.dstu3.model.Identifier) currentMbi.getValue();
        assertEquals(MBI_ID, currentId.getSystem());
        assertEquals("current-mbi", currentId.getValue());

        assertFalse(currentId.getExtension().isEmpty());
        org.hl7.fhir.dstu3.model.Extension currentExtension = currentId.getExtension().get(0);
        assertEquals(CURRENCY_IDENTIFIER, currentExtension.getUrl());
        assertEquals(CURRENT_MBI, ((org.hl7.fhir.dstu3.model.Coding) currentExtension.getValue()).getCode());
    }

    private void checkHistoricalMbi(org.hl7.fhir.dstu3.model.Extension historicalExtension, String id) {
        org.hl7.fhir.dstu3.model.Identifier historicalId = (org.hl7.fhir.dstu3.model.Identifier) historicalExtension.getValue();
        assertEquals(MBI_ID, historicalId.getSystem());
        assertEquals(id, historicalId.getValue());

        assertFalse(historicalId.getExtension().isEmpty());
        org.hl7.fhir.dstu3.model.Extension historicExtension = historicalId.getExtension().get(0);
        assertEquals(CURRENCY_IDENTIFIER, historicExtension.getUrl());
        assertEquals(HISTORIC_MBI, ((org.hl7.fhir.dstu3.model.Coding) historicExtension.getValue()).getCode());
    }
}
