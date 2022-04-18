package gov.cms.ab2d.api.controller.common;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.service.InvalidContractException;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.fhir.FhirVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiCommonTest {

    private static final String CONTRACT_NUMBER = "X1234";

    final PdpClient pdpClient;

    final ApiCommon apiCommon;

    ApiCommonTest() {
        Contract contract = new Contract();
        contract.setContractNumber(CONTRACT_NUMBER);
        PdpClient pdpClientTmp = new PdpClient();
        pdpClientTmp.setContract(contract);
        pdpClient = pdpClientTmp;

        apiCommon = buildApiCommon();
    }

    @Test
    void unattestedCheck() {
        InvalidContractException ice = assertThrows(InvalidContractException.class, () ->
                apiCommon.checkValidCreateJob(null, CONTRACT_NUMBER, null,
        "resource_type", "jpg", FhirVersion.STU3));
        assertNotNull(ice);
        assertTrue(ice.getMessage().contains(CONTRACT_NUMBER + " is not attested."));
    }

    @Test
    void contractNumberMismatch() {
        final String bogusContractNumber = "BOGUS";
        InvalidContractException ice = assertThrows(InvalidContractException.class, () ->
                apiCommon.checkValidCreateJob(null, bogusContractNumber, null,
                        "resource_type", "jpg", FhirVersion.STU3));
        assertNotNull(ice);
        assertTrue(ice.getMessage().contains(bogusContractNumber + " not associated with internal id"));
    }

    private ApiCommon buildApiCommon() {
        return new ApiCommon(null, null, null, buildPdpClientService());
    }

    private PdpClientService buildPdpClientService() {
        PdpClientService retPdpService = mock(PdpClientService.class);
        when(retPdpService.getCurrentClient()).thenReturn(pdpClient);
        return retPdpService;
    }
}
