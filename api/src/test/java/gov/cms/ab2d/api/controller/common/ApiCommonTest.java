package gov.cms.ab2d.api.controller.common;

import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.service.InvalidClientInputException;
import gov.cms.ab2d.common.service.InvalidContractException;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.fhir.FhirVersion;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
gimport org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiCommonTest {

    private static final String CONTRACT_NUMBER = "X1234";
    private static final Long CONTRACT_ID = 100L;

    final PdpClient pdpClient;

    final ApiCommon apiCommon;

    ApiCommonTest() {
        Contract contract = new Contract();
        contract.setContractNumber(CONTRACT_NUMBER);
        contract.setId(CONTRACT_ID);
        PdpClient pdpClientTmp = new PdpClient();
        pdpClientTmp.setContractId(contract.getId());
        pdpClient = pdpClientTmp;
        ContractService contractService = mock(ContractService.class);
        when(contractService.getContractByContractId(anyLong())).thenReturn(contract);

        apiCommon = buildApiCommon(contractService);
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

    @Test
    void testCheckSinceTime() {
        assertDoesNotThrow(() -> {
            apiCommon.checkSinceTime(null);
        });

        OffsetDateTime time1 = OffsetDateTime.of(9999, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
        assertThrows(InvalidClientInputException.class, () -> {
            apiCommon.checkSinceTime(time1);
        });

        OffsetDateTime time2 = OffsetDateTime.of(1, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
        assertThrows(InvalidClientInputException.class, () -> {
            apiCommon.checkSinceTime(time2);
        });
    }

    @Test
    void testCheckIfContractAttested() {
        assertThrows(IllegalStateException.class, () -> {
            apiCommon.checkIfContractAttested(null, CONTRACT_NUMBER);
        });
    }

    private ApiCommon buildApiCommon(ContractService contractService) {
        return new ApiCommon(null, null, null, buildPdpClientService(), contractService);
    }

    private PdpClientService buildPdpClientService() {
        PdpClientService retPdpService = mock(PdpClientService.class);
        when(retPdpService.getCurrentClient()).thenReturn(pdpClient);
        return retPdpService;
    }
}
