package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.model.Identifiers;
import gov.cms.ab2d.common.util.fhir.FhirUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.fhir.IdentifierUtils.CURRENCY_IDENTIFIER;
import static gov.cms.ab2d.worker.processor.ContractProcessorImpl.ID_EXT;
import static gov.cms.ab2d.worker.processor.coverage.CoverageMappingCallable.*;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

class ContractProcessorImplTest {

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

        FhirUtils.addMbiIdsToEobs(singletonList(eob), coverageSummaries, STU3);

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
