package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.dto.*;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import java.util.List;

import static gov.cms.ab2d.common.util.Constants.ADMIN_ROLE;
import static gov.cms.ab2d.common.util.Constants.SPONSOR_ROLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
@Testcontainers
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    private DataSetup dataSetup;

    @BeforeEach
    public void setup() {
        dataSetup.createRole(SPONSOR_ROLE);
    }

    @AfterEach
    public void teardown() {
        dataSetup.cleanup();
    }

    @Test
    void testCreateUser() {
        UserDTO user = buildUserDTO("Test", SPONSOR_ROLE);

        UserDTO createdUser = userService.createUser(user);
        dataSetup.queueForCleanup(userService.getUserByUsername("test@test.com"));

        assertEquals(user.getUsername(), createdUser.getUsername());
        assertEquals(user.getEmail(), createdUser.getEmail());
        assertEquals(user.getFirstName(), createdUser.getFirstName());
        assertEquals(user.getLastName(), createdUser.getLastName());
        assertEquals(user.getEnabled(), createdUser.getEnabled());
        assertEquals(user.getContract(), createdUser.getContract());
        assertEquals(SPONSOR_ROLE, createdUser.getRole());
    }

    @Test
    void testCreateDuplicateUser() {
        UserDTO user = buildUserDTO("Test", SPONSOR_ROLE);

        userService.createUser(user);
        dataSetup.queueForCleanup(userService.getUserByUsername("test@test.com"));

        var exceptionThrown = Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            user.setEmail("anotherEmail@test.com");
            userService.createUser(user);
        });
        assertEquals("could not execute statement; SQL [n/a]; constraint [uc_user_account_username]; " +
                "nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement",
                exceptionThrown.getMessage());
    }

    private UserDTO buildUserDTO(String test, String sponsorRole) {
        Contract contract = dataSetup.setupContract(test);

        return createUser(contract, sponsorRole);
    }

    private UserDTO createUser(Contract contract, @Nullable String roleName) {
        UserDTO user = new UserDTO();
        user.setEmail("test@test.com");
        user.setUsername("test@test.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        ContractDTO contractDTO = new ContractDTO(contract.getContractNumber(), contract.getContractName(),
                contract.getAttestedOn().toString());
        user.setContract(contractDTO);
        user.setRole(roleName);

        return user;
    }

    @Test
    void testUpdateUser() {
        UserDTO user = buildUserDTO("Test", SPONSOR_ROLE);

        UserDTO createdUser = userService.createUser(user);
        dataSetup.queueForCleanup(userService.getUserByUsername("test@test.com"));

        createdUser.setEmail("newTest@test.com");
        createdUser.setFirstName("New");
        createdUser.setLastName("User");
        createdUser.setEnabled(false);
        ContractDTO contractDTO = buildContractDTO(dataSetup.setupContract("T12345"));
        createdUser.setContract(contractDTO);
        createdUser.setRole(SPONSOR_ROLE);

        UserDTO updatedUser = userService.updateUser(createdUser);

        assertEquals(createdUser.getUsername(), updatedUser.getUsername());
        assertEquals(createdUser.getEmail(), updatedUser.getEmail());
        assertEquals(createdUser.getFirstName(), updatedUser.getFirstName());
        assertEquals(createdUser.getLastName(), updatedUser.getLastName());
        assertEquals(createdUser.getEnabled(), updatedUser.getEnabled());
        assertEquals(createdUser.getContract().getContractName(), updatedUser.getContract().getContractName());
        assertEquals(createdUser.getContract().getContractNumber(), updatedUser.getContract().getContractNumber());
        assertEquals(createdUser.getRole(), updatedUser.getRole());
    }

    private ContractDTO buildContractDTO(Contract contract) {
        ContractDTO contractDTO = new ContractDTO();
        contractDTO.setContractName(contract.getContractName());
        contractDTO.setContractNumber(contract.getContractNumber());
        contractDTO.setAttestedOn(contract.getAttestedOn().toString());
        return contractDTO;
    }

    @Test
    void testUpdateUserAddRole() {
        UserDTO user = buildUserDTO("Test", null);

        assertNull(user.getRole());

        UserDTO createdUser = userService.createUser(user);
        dataSetup.queueForCleanup(userService.getUserByUsername("test@test.com"));

        createdUser.setRole(SPONSOR_ROLE);

        UserDTO updatedUser = userService.updateUser(createdUser);

        assertEquals(createdUser.getRole(), updatedUser.getRole());
    }

    @Test
    void testUpdateUserRemoveRole() {
        UserDTO user = buildUserDTO("TEST", SPONSOR_ROLE);

        assertEquals(SPONSOR_ROLE, user.getRole());

        UserDTO createdUser = userService.createUser(user);
        dataSetup.queueForCleanup(userService.getUserByUsername("test@test.com"));

        createdUser.setRole(null);

        UserDTO updatedUser = userService.updateUser(createdUser);

        assertNull(updatedUser.getRole());
    }

    @Test
    void testSetupUserAndRolesInSecurityContext() {
        HttpServletRequest httpServletRequest = new MockHttpServletRequest();

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(ADMIN_ROLE));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "ADMIN", null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserDTO user = buildUserDTO("Test", SPONSOR_ROLE);
        UserDTO createdUser = userService.createUser(user);
        dataSetup.queueForCleanup(userService.getUserByUsername("test@test.com"));

        String contractNumber = createdUser.getContract().getContractNumber();

        userService.setupUserImpersonation(contractNumber, httpServletRequest);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("test@test.com", authentication.getPrincipal());
        assertEquals(1, authentication.getAuthorities().size());
        GrantedAuthority grantedAuthority = authentication.getAuthorities().iterator().next();
        assertEquals(SPONSOR_ROLE, grantedAuthority.getAuthority());
    }

    @Test
    void testSetupUserAndRolesInSecurityContextBadUser() {
        HttpServletRequest httpServletRequest = new MockHttpServletRequest();
        var exceptionThrown = Assertions.assertThrows(ResourceNotFoundException.class,
                () -> userService.setupUserImpersonation("UserDoesNotExist", httpServletRequest));
        assertEquals("User is not present in our database", exceptionThrown.getMessage());
    }

    @Test
    void testEnableUser() {
        UserDTO user = buildUserDTO("Test", SPONSOR_ROLE);
        user.setEnabled(false);

        userService.createUser(user);
        dataSetup.queueForCleanup(userService.getUserByUsername("test@test.com"));

        UserDTO updatedUser = userService.enableUser(user.getUsername());
        assertEquals(true, updatedUser.getEnabled());
    }

    @Test
    void testDisableUser() {
        UserDTO user = buildUserDTO("Test", SPONSOR_ROLE);
        userService.createUser(user);
        dataSetup.queueForCleanup(userService.getUserByUsername("test@test.com"));

        UserDTO updatedUser = userService.disableUser(user.getUsername());
        assertEquals(false, updatedUser.getEnabled());
    }
}
