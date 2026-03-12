package gov.cms.ab2d.api.controller.common;

import gov.cms.ab2d.api.security.EndpointNotAvailableException;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.service.InvalidClientInputException;
import gov.cms.ab2d.common.service.InvalidContractException;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.fhir.FhirVersion;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import static gov.cms.ab2d.common.util.Constants.SINCE_EARLIEST_DATE_TIME;
import static gov.cms.ab2d.common.util.PropertyConstants.V3_ON;
import static gov.cms.ab2d.common.util.PropertyConstants.V3_ALLOWLISTED_CONTRACTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiCommonTest {

    private static final String CONTRACT_NUMBER = "X1234";
    private static final Long CONTRACT_ID = 100L;
    private static final OffsetDateTime SINCE_BEFORE_EARLIEST_DATE_TIME = SINCE_EARLIEST_DATE_TIME.minusMonths(1);

    final PdpClient pdpClient;

    final ApiCommon apiCommon;

    ContractService contractService;
    PropertiesService propertiesService;
    PdpClientService pdpService;

    ApiCommonTest() {
        Contract contract = new Contract();
        contract.setContractNumber(CONTRACT_NUMBER);
        contract.setId(CONTRACT_ID);
        PdpClient pdpClientTmp = new PdpClient();
        pdpClientTmp.setContractId(contract.getId());
        pdpClient = pdpClientTmp;
        contractService = mock(ContractService.class);
        propertiesService = mock(PropertiesService.class);
        pdpService = mock(PdpClientService.class);

        when(contractService.getContractByContractId(anyLong())).thenReturn(contract);

        when(pdpService.getCurrentClient()).thenReturn(pdpClient);

        apiCommon = new ApiCommon(null, null, propertiesService, pdpService, contractService);
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
        OffsetDateTime oneMonthInTheFuture = OffsetDateTime.now().plusMonths(1);
        assertThrows(InvalidClientInputException.class, () -> apiCommon.checkSinceTime(oneMonthInTheFuture));
        assertThrows(InvalidClientInputException.class, () -> apiCommon.checkSinceTime(SINCE_BEFORE_EARLIEST_DATE_TIME));
    }

    @Test
    void checkUntilTimeTest() {
        OffsetDateTime currentDate = OffsetDateTime.now();
        assertDoesNotThrow(() -> apiCommon.checkUntilTime(SINCE_EARLIEST_DATE_TIME, currentDate, FhirVersion.R4));
        assertDoesNotThrow(() -> apiCommon.checkUntilTime(SINCE_EARLIEST_DATE_TIME, null, FhirVersion.R4));
        assertThrows(InvalidClientInputException.class, () -> apiCommon.checkUntilTime(SINCE_EARLIEST_DATE_TIME, currentDate, FhirVersion.STU3));
        assertThrows(InvalidClientInputException.class, () -> apiCommon.checkUntilTime(currentDate, SINCE_EARLIEST_DATE_TIME, FhirVersion.R4));
        assertThrows(InvalidClientInputException.class, () -> apiCommon.checkUntilTime(null, SINCE_BEFORE_EARLIEST_DATE_TIME, FhirVersion.R4));
    }

    @Test
    void getServiceDateTest() {
        String validTypeFilter = "ExplanationOfBenefit%3Fservice-date%3Dgt2026-02-01";
        String invalidMissingResourceType = "service-date=2012-01-01";
        String invalidWrongResourceType = "Patient?service-date=2012-01-01";
        String invalidQueryParam = "ExplanationOfBenefit%3F_tag%3DNationalClaimsHistory";
        assertDoesNotThrow(() -> apiCommon.getServiceDates(null));
        assertDoesNotThrow(() -> apiCommon.getServiceDates(validTypeFilter));
        assertThrows(InvalidClientInputException.class, () -> apiCommon.getServiceDates(invalidMissingResourceType));
        assertThrows(InvalidClientInputException.class, () -> apiCommon.getServiceDates(invalidWrongResourceType));
        assertThrows(InvalidClientInputException.class, () -> apiCommon.getServiceDates(invalidQueryParam));
    }

    @Test
    void checkServiceDateTest() {
        List<String> validServiceDates = List.of("gt2020-01-01", "le2020-02-01");
        List<String> validYearOnly = List.of("eq2022");
        List<String> invalidOperatorCode = List.of("zz2020-01-01");
        List<String> invalidFormat = List.of("lt20200101");
        List<String> invalidNotRealDate = List.of("eq2020-22-44");
        assertDoesNotThrow(() -> apiCommon.checkServiceDates(null));
        assertDoesNotThrow(() -> apiCommon.checkServiceDates(validServiceDates));
        assertDoesNotThrow(() -> apiCommon.checkServiceDates(validYearOnly));
        assertThrows(InvalidClientInputException.class, () -> apiCommon.checkServiceDates(invalidOperatorCode));
        assertThrows(InvalidClientInputException.class, () -> apiCommon.checkServiceDates(invalidFormat));
        assertThrows(InvalidClientInputException.class, () -> apiCommon.checkServiceDates(invalidNotRealDate));
    }
  
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

    @Test
    void v3ContractIsAllowListed() {
        when(propertiesService.getProperty(eq(V3_ON), any())).thenReturn("true");
        when(propertiesService.getProperty(eq(V3_ALLOWLISTED_CONTRACTS), any())).thenReturn("S1234,S5555");
        assertDoesNotThrow(() -> apiCommon.checkContractIsAllowListedForV3("S1234"));
        assertDoesNotThrow(() -> apiCommon.checkContractIsAllowListedForV3("S5555"));
    }

    @Test
    void v3ContractNotAllowListed() {
        when(propertiesService.getProperty(eq(V3_ON), any())).thenReturn("false");
        when(propertiesService.getProperty(eq(V3_ALLOWLISTED_CONTRACTS), any())).thenReturn("S1234,S5555");
        assertThrows(EndpointNotAvailableException.class, () -> apiCommon.checkContractIsAllowListedForV3("S9999"));
    }

    @Test
    void v3NotEnabled() {
        when(propertiesService.getProperty(eq(V3_ON), any())).thenReturn("false");
        assertThrows(EndpointNotAvailableException.class, () -> apiCommon.checkContractIsAllowListedForV3("Z1234"));
    }

}
