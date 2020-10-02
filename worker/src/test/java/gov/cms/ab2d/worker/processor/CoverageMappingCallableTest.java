package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.worker.processor.domainmodel.CoverageMapping;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static gov.cms.ab2d.worker.processor.CoverageMappingCallable.BENEFICIARY_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class CoverageMappingCallableTest {

    private BFDClient bfdClient;

    @BeforeEach
    public void before() {

        bfdClient = Mockito.mock(BFDClient.class);

    }

    @DisplayName("Successfully completing marks as done and transfers results")
    @Test
    void callableFunctions() {

        Bundle bundle1 = buildBundle(0, 10);
        bundle1.setLink(Collections.singletonList(new Bundle.BundleLinkComponent().setRelation(Bundle.LINK_NEXT)));

        Bundle bundle2 = buildBundle(10, 20);

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(any(Bundle.class))).thenReturn(bundle2);

        Contract contract = new Contract();
        contract.setContractNumber("TESTING");
        contract.setContractName("TESTING");

        CoveragePeriod period = new CoveragePeriod();
        period.setContract(contract);
        period.setYear(2020);
        period.setMonth(1);

        CoverageMapping mapping = new CoverageMapping(period);
        CoverageMappingCallable callable = new CoverageMappingCallable(mapping, bfdClient);

        assertFalse(callable.isCompleted());

        CoverageMapping results = callable.call();
        assertEquals(mapping, results);

        assertTrue(callable.isCompleted());
        assertTrue(mapping.isSuccessful());
        assertEquals(20, results.getBeneficiaryIds().size());
    }

    @DisplayName("Exceptional behavior leads to failure")
    @Test
    void exceptionCaught() {

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenThrow(new RuntimeException("exception"));

        Contract contract = new Contract();
        contract.setContractNumber("TESTING");
        contract.setContractName("TESTING");

        CoveragePeriod period = new CoveragePeriod();
        period.setContract(contract);
        period.setYear(2020);
        period.setMonth(1);

        CoverageMapping mapping = new CoverageMapping(period);
        CoverageMappingCallable callable = new CoverageMappingCallable(mapping, bfdClient);

        try {
            callable.call();
        } catch (Exception exception) {
            // ignore exception for sake of test
        }

        assertFalse(mapping.isSuccessful());
        assertTrue(callable.isCompleted());
        assertTrue(mapping.getLastLog().contains("Unable"));
    }

    private Bundle buildBundle(int startIndex, int endIndex) {
        Bundle bundle1 = new Bundle();

        for (int i = startIndex; i < endIndex; i++) {
            Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
            Patient patient = new Patient();

            Identifier identifier = new Identifier();
            identifier.setSystem(BENEFICIARY_ID);
            identifier.setValue("test-" + i);

            patient.setIdentifier(Collections.singletonList(identifier));
            component.setResource(patient);

            bundle1.addEntry(component);
        }
        return bundle1;
    }
}
