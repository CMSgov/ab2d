package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.dto.PdpClientDTO;
import gov.cms.ab2d.common.feign.ContractFeignClient;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import java.util.List;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static gov.cms.ab2d.common.model.Role.ADMIN_ROLE;
import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
@Testcontainers
class PdpClientServiceTest {

    @MockBean
    ContractFeignClient contractFeignClient;

    @Autowired
    private ContractService contractService;

    @Autowired
    private PdpClientService pdpClientService;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    private DataSetup dataSetup;

    private static final String TEST_CLIENT = "test@test.com";
    private static final String ENABLED_CLIENT = "enabled@test.com";
    private static final String DISABLED_CLIENT = "disabled@test.com";

    @AfterEach
    public void teardown() {
        dataSetup.cleanup();
    }

    @Test
    void testFindEnabledClients() {
        PdpClientDTO client = buildClientDTO("Enabled", ENABLED_CLIENT, SPONSOR_ROLE);
        client.setEnabled(true);

        pdpClientService.createClient(client);
        dataSetup.queueForCleanup(pdpClientService.getClientById(ENABLED_CLIENT));

        client = buildClientDTO("Disabled", DISABLED_CLIENT, SPONSOR_ROLE);
        client.setEnabled(false);

        pdpClientService.createClient(client);
        dataSetup.queueForCleanup(pdpClientService.getClientById(DISABLED_CLIENT));

        Contract contract = contractService.getContractByContractNumber("Enabled").get();
        List<Contract> contracts = pdpClientService.getAllEnabledContracts();

        assertEquals(10, contracts.size());
        assertTrue(contracts.stream().filter(c -> c.getContractNumber().equals(contract.getContractNumber())).findAny().isPresent());
    }

    @Test
    void testCreateClient() {
        PdpClientDTO client = buildClientDTO("Test", TEST_CLIENT, SPONSOR_ROLE);

        PdpClientDTO createdClient = pdpClientService.createClient(client);
        dataSetup.queueForCleanup(pdpClientService.getClientById("test@test.com"));

        assertEquals(client.getClientId(), createdClient.getClientId());
        assertEquals(client.getEnabled(), createdClient.getEnabled());
        assertEquals(client.getContract(), createdClient.getContract());
        assertEquals(SPONSOR_ROLE, createdClient.getRole());
    }

    @Test
    void testCreateDuplicateClient() {
        PdpClientDTO client = buildClientDTO("Test", TEST_CLIENT, SPONSOR_ROLE);

        pdpClientService.createClient(client);
        dataSetup.queueForCleanup(pdpClientService.getClientById("test@test.com"));

        var exceptionThrown = Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            pdpClientService.createClient(client);
        });
        // assertEquals("could not execute statement; SQL [n/a]; constraint [uc_user_account_username]; " +
        //                 "nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement",
        //         exceptionThrown.getMessage());

        // This changed because the migration to Springboot 3.0.0 updated Hibernate from version 5 to 6.
        // This feels like a poorly written assertion to me? Should we ever be hard-coding an exact error message?
        assertEquals("could not execute statement; SQL [n/a]; constraint [uc_user_account_username]", exceptionThrown.getMessage());
    }

    @Test
    void testUpdateClient() {
        PdpClientDTO client = buildClientDTO("Test", TEST_CLIENT, SPONSOR_ROLE);

        PdpClientDTO createdClient = pdpClientService.createClient(client);
        dataSetup.queueForCleanup(pdpClientService.getClientById("test@test.com"));

        createdClient.setEnabled(false);
        ContractDTO contractDTO = buildContractDTO(dataSetup.setupContract("T12345", AB2D_EPOCH.toOffsetDateTime()));
        createdClient.setContract(contractDTO);
        createdClient.setRole(SPONSOR_ROLE);

        PdpClientDTO updatedClient = pdpClientService.updateClient(createdClient);

        assertEquals(createdClient.getClientId(), updatedClient.getClientId());
        assertEquals(createdClient.getEnabled(), updatedClient.getEnabled());
        assertEquals(createdClient.getContract().getContractName(), updatedClient.getContract().getContractName());
        assertEquals(createdClient.getContract().getContractNumber(), updatedClient.getContract().getContractNumber());
        assertEquals(createdClient.getRole(), updatedClient.getRole());
    }

    @Test
    void testUpdateClientAddRole() {
        PdpClientDTO client = buildClientDTO("Test", TEST_CLIENT,null);

        assertNull(client.getRole());

        PdpClientDTO createdClient = pdpClientService.createClient(client);
        dataSetup.queueForCleanup(pdpClientService.getClientById("test@test.com"));

        createdClient.setRole(SPONSOR_ROLE);

        PdpClientDTO updatedClient = pdpClientService.updateClient(createdClient);

        assertEquals(createdClient.getRole(), updatedClient.getRole());
    }

    @Test
    void testUpdateClientRemoveRole() {
        PdpClientDTO client = buildClientDTO("TEST", TEST_CLIENT, SPONSOR_ROLE);

        assertEquals(SPONSOR_ROLE, client.getRole());

        PdpClientDTO createdClient = pdpClientService.createClient(client);
        dataSetup.queueForCleanup(pdpClientService.getClientById("test@test.com"));

        createdClient.setRole(null);

        PdpClientDTO updatedClient = pdpClientService.updateClient(createdClient);

        assertNull(updatedClient.getRole());
    }

    @Test
    void testSetupClientAndRolesInSecurityContext() {
        HttpServletRequest httpServletRequest = new MockHttpServletRequest();

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(ADMIN_ROLE));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "ADMIN", null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));
        SecurityContextHolder.getContext().setAuthentication(auth);

        PdpClientDTO client = buildClientDTO("Test", TEST_CLIENT, SPONSOR_ROLE);
        PdpClientDTO createdClient = pdpClientService.createClient(client);
        dataSetup.queueForCleanup(pdpClientService.getClientById("test@test.com"));

        String contractNumber = createdClient.getContract().getContractNumber();

        pdpClientService.setupClientImpersonation(contractNumber, httpServletRequest);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("test@test.com", authentication.getPrincipal());
        assertEquals(1, authentication.getAuthorities().size());
        GrantedAuthority grantedAuthority = authentication.getAuthorities().iterator().next();
        assertEquals(SPONSOR_ROLE, grantedAuthority.getAuthority());
    }

    @Test
    void testSetupClientAndRolesInSecurityContextBadClient() {
        buildContract("ClientDoesNotExist");
        HttpServletRequest httpServletRequest = new MockHttpServletRequest();
        var exceptionThrown = Assertions.assertThrows(ResourceNotFoundException.class,
                () -> pdpClientService.setupClientImpersonation("ClientDoesNotExist", httpServletRequest));
        assertEquals("Client is not present in our database", exceptionThrown.getMessage());
    }

    @Test
    void testEnableClient() {
        PdpClientDTO client = buildClientDTO("Test", TEST_CLIENT, SPONSOR_ROLE);
        client.setEnabled(false);

        pdpClientService.createClient(client);
        dataSetup.queueForCleanup(pdpClientService.getClientById("test@test.com"));

        PdpClientDTO updatedClient = pdpClientService.enableClient(client.getContract().getContractNumber());
        assertEquals(true, updatedClient.getEnabled());
    }

    @Test
    void testDisableClient() {
        PdpClientDTO client = buildClientDTO("Test", TEST_CLIENT, SPONSOR_ROLE);
        pdpClientService.createClient(client);
        dataSetup.queueForCleanup(pdpClientService.getClientById("test@test.com"));

        PdpClientDTO updatedClient = pdpClientService.disableClient(client.getContract().getContractNumber());
        assertEquals(false, updatedClient.getEnabled());
    }

    private PdpClientDTO buildClientDTO(String contractNumber, String clientId, String sponsorRole) {
        Contract contract = buildContract(contractNumber);

        return createClient(contract, clientId, sponsorRole);
    }

    @NotNull
    private Contract buildContract(String contractNumber) {
        Contract contract = dataSetup.setupContract(contractNumber, AB2D_EPOCH.toOffsetDateTime());

        when(contractFeignClient.getContractByNumber(eq(contractNumber))).thenReturn(contract.toDTO());
        when(contractFeignClient.getContracts(eq(contract.getId()))).thenReturn(List.of(contract.toDTO()));
        return contract;
    }

    private ContractDTO buildContractDTO(Contract contract) {
        when(contractFeignClient.getContractByNumber(eq(contract.getContractNumber()))).thenReturn(contract.toDTO());
        when(contractFeignClient.getContracts(eq(contract.getId()))).thenReturn(List.of(contract.toDTO()));
        return contract.toDTO();
    }

    private PdpClientDTO createClient(Contract contract, String clientId, @Nullable String roleName) {
        PdpClientDTO client = new PdpClientDTO();
        client.setClientId(clientId);
        client.setOrganization(clientId);
        client.setEnabled(true);
        client.setContract(contract.toDTO());
        client.setRole(roleName);

        return client;
    }
}
