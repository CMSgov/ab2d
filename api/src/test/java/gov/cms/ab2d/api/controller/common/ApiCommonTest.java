package gov.cms.ab2d.api.controller.common;

import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.service.InvalidClientInputException;
import gov.cms.ab2d.common.service.InvalidContractException;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.fhir.FhirVersion;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static gov.cms.ab2d.common.util.Constants.SINCE_EARLIEST_DATE_TIME;
import static org.junit.jupiter.api.Assertions.*;
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
                apiCommon.checkValidCreateJob(null, CONTRACT_NUMBER, null, null,
                        "resource_type", "jpg", FhirVersion.STU3));
        assertNotNull(ice);
        assertTrue(ice.getMessage().contains(CONTRACT_NUMBER + " is not attested."));
    }

    @Test
    void contractNumberMismatch() {
        final String bogusContractNumber = "BOGUS";
        InvalidContractException ice = assertThrows(InvalidContractException.class, () ->
                apiCommon.checkValidCreateJob(null, bogusContractNumber, null, null,
                        "resource_type", "jpg", FhirVersion.STU3));
        assertNotNull(ice);
        assertTrue(ice.getMessage().contains(bogusContractNumber + " not associated with internal id"));
    }

    @Test
    void checkSinceTimeTest() {
        assertDoesNotThrow(() -> apiCommon.checkSinceTime(SINCE_EARLIEST_DATE_TIME));
        assertDoesNotThrow(() -> apiCommon.checkSinceTime(null));
        assertThrows(InvalidClientInputException.class, () -> apiCommon.checkSinceTime(OffsetDateTime.now().plusMonths(1)));
        assertThrows(InvalidClientInputException.class, () -> apiCommon.checkSinceTime(SINCE_EARLIEST_DATE_TIME.minusMonths(1)));
    }

    @Test
    void checkUntilTimeTest() {
        OffsetDateTime currentDate = OffsetDateTime.now();
        assertDoesNotThrow(() -> apiCommon.checkUntilTime(SINCE_EARLIEST_DATE_TIME, currentDate, FhirVersion.R4));
        assertDoesNotThrow(() -> apiCommon.checkUntilTime(SINCE_EARLIEST_DATE_TIME, null, FhirVersion.R4));
        assertThrows(InvalidClientInputException.class, () -> apiCommon.checkUntilTime(SINCE_EARLIEST_DATE_TIME, currentDate, FhirVersion.STU3));
        assertThrows(InvalidClientInputException.class, () -> apiCommon.checkUntilTime(currentDate, SINCE_EARLIEST_DATE_TIME, FhirVersion.R4));
        assertThrows(InvalidClientInputException.class, () -> apiCommon.checkUntilTime(null, SINCE_EARLIEST_DATE_TIME.minusMonths(1), FhirVersion.R4));
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
