package gov.cms.ab2d.api.controller.common;

import gov.cms.ab2d.common.dto.PdpClientDTO;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.dto.StartJobDTO;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.service.InvalidContractException;
import gov.cms.ab2d.common.service.JobService;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.fhir.FhirVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiCommonTest {

    private static final String CONTRACT_NUMBER = "X1234";

    PdpClient pdpClient;

    @BeforeEach
    void startup() {
        Contract contract = new Contract();
        contract.setContractNumber(CONTRACT_NUMBER);
        PdpClient pdpClientTmp = new PdpClient();
        pdpClientTmp.setContract(contract);
        pdpClient = pdpClientTmp;
    }

    @Test
    void unattestedCheck() {
        ApiCommon apiCommon = buildApiCommon();
        InvalidContractException ice = assertThrows(InvalidContractException.class, () ->
                apiCommon.checkValidCreateJob(null, CONTRACT_NUMBER, null,
        "resource_type", "jpg", FhirVersion.STU3));
        assertNotNull(ice);
        assertTrue(ice.getMessage().contains(CONTRACT_NUMBER + " is not attested."));
    }

    @Test
    void contractNumberMismatch() {
        final String bogusContractNumber = "BOGUS";
        ApiCommon apiCommon = buildApiCommon();
        InvalidContractException ice = assertThrows(InvalidContractException.class, () ->
                apiCommon.checkValidCreateJob(null, bogusContractNumber, null,
                        "resource_type", "jpg", FhirVersion.STU3));
        assertNotNull(ice);
        assertTrue(ice.getMessage().contains(bogusContractNumber + " not associated with internal id"));
    }

    private ApiCommon buildApiCommon() {
        return new ApiCommon(buildEventLogger(), buildJobService(), buildPropertiesService(), buildPdpClientService());
    }

    LogManager buildEventLogger() {
        return new LogManager(null, null, null);
    }

    private JobService buildJobService() {
        return new JobService() {
            @Override
            public Job createJob(StartJobDTO startJobDTO) {
                return null;
            }

            @Override
            public void cancelJob(String jobUuid, String organization) {

            }

            @Override
            public Job getAuthorizedJobByJobUuid(String jobUuid, String organization) {
                return null;
            }

            @Override
            public Job getJobByJobUuid(String jobUuid) {
                return null;
            }

            @Override
            public Job updateJob(Job job) {
                return null;
            }

            @Override
            public Resource getResourceForJob(String jobUuid, String fileName, String organization) {
                return null;
            }

            @Override
            public void deleteFileForJob(File file, String jobUuid) {

            }

            @Override
            public int activeJobs(String organization) {
                return 0;
            }

            @Override
            public List<String> getActiveJobIds(String organization) {
                return null;
            }
        };
    }

    private PropertiesService buildPropertiesService() {
        return new PropertiesService() {
            @Override
            public boolean isInMaintenanceMode() {
                return false;
            }

            @Override
            public List<Properties> getAllProperties() {
                return null;
            }

            @Override
            public List<PropertiesDTO> getAllPropertiesDTO() {
                return null;
            }

            @Override
            public Properties getPropertiesByKey(String key) {
                return null;
            }

            @Override
            public List<PropertiesDTO> updateProperties(List<PropertiesDTO> propertiesDTOs) {
                return null;
            }

            @Override
            public boolean isToggleOn(String toggleName) {
                return false;
            }
        };
    }

    private PdpClientService buildPdpClientService() {
        return new PdpClientService() {
            @Override
            public List<Contract> getAllEnabledContracts() {
                return null;
            }

            @Override
            public PdpClient getCurrentClient() {
                return pdpClient;
            }

            @Override
            public PdpClient getClientById(String clientId) {
                return null;
            }

            @Override
            public PdpClientDTO createClient(PdpClientDTO pdpClientDTO) {
                return null;
            }

            @Override
            public PdpClientDTO updateClient(PdpClientDTO pdpClientDTO) {
                return null;
            }

            @Override
            public void setupClientImpersonation(String contractNumber, HttpServletRequest request) {

            }

            @Override
            public void setupClientAndRolesInSecurityContext(PdpClient pdpClient, HttpServletRequest request) {

            }

            @Override
            public List<GrantedAuthority> getGrantedAuth(PdpClient pdpClient) {
                return null;
            }

            @Override
            public PdpClientDTO enableClient(String contractNumber) {
                return null;
            }

            @Override
            public PdpClientDTO disableClient(String contractNumber) {
                return null;
            }

            @Override
            public PdpClientDTO getClient(String contractNumber) {
                return null;
            }
        };
    }
}
