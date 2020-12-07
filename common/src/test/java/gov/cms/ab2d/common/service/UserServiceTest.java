package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.dto.*;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import gov.cms.ab2d.common.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
@Testcontainers
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleService roleService;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    private DataSetup dataSetup;

    @BeforeEach
    public void init() {
        userRepository.deleteAll();
    }

    @Test
    public void testUser() {
        User user = userService.getCurrentUser();

        assertNull(user); // no authentication for now, so will be null
    }

    @Test
    public void testCreateUser() {
        UserDTO user = buildUserDTO("Test", SPONSOR_ROLE);

        UserDTO createdUser = userService.createUser(user);
        Assert.assertEquals(createdUser.getUsername(), user.getUsername());
        Assert.assertEquals(createdUser.getEmail(), user.getEmail());
        Assert.assertEquals(createdUser.getFirstName(), user.getFirstName());
        Assert.assertEquals(createdUser.getLastName(), user.getLastName());
        Assert.assertEquals(createdUser.getEnabled(), user.getEnabled());
        Assert.assertEquals(createdUser.getContract(), user.getContract());
        Assert.assertEquals(createdUser.getRole(), SPONSOR_ROLE);
    }

    @Test
    public void testCreateDuplicateUser() {
        UserDTO user = buildUserDTO("Test", SPONSOR_ROLE);

        userService.createUser(user);
        var exceptionThrown = Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            user.setEmail("anotherEmail@test.com");
            userService.createUser(user);
        });
        assertThat(exceptionThrown.getMessage(), is("could not execute statement; SQL [n/a]; constraint [uc_user_account_username]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement"));
    }

//    @Test - todo: fix or delete
    private UserDTO buildUserDTO(String test, String sponsorRole) {
        Contract contract = dataSetup.setupContract(test);

        return createUser(contract, sponsorRole);
    }

    @Test
    public void testCreateDuplicateUserByEmail() {
        UserDTO user = buildUserDTO("N123", SPONSOR_ROLE);

        userService.createUser(user);

        user.setUsername("something_different");
        var exceptionThrown = Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            userService.createUser(user);
        });
        assertThat(exceptionThrown.getMessage(), is("could not execute statement; SQL [n/a]; constraint [uc_user_account_email]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement"));
    }

    private UserDTO createUser(Contract contract, @Nullable String roleName) {
        UserDTO user = new UserDTO();
        user.setEmail("test@test.com");
        user.setUsername("test@test.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        ContractDTO contractDTO = new ContractDTO();
        user.setContract(contractDTO);
        if(roleName != null) {
            Role role = roleService.findRoleByName(roleName);
            user.setRole(role.getName());
        }

        // todo: get rid of
        user.setUsername(contract.getContractName());

        return user;
    }

    @Test
    public void testUpdateUser() {
        UserDTO user = buildUserDTO("Test", SPONSOR_ROLE);

        UserDTO createdUser = userService.createUser(user);

        createdUser.setEmail("newTest@test.com");
        createdUser.setFirstName("New");
        createdUser.setLastName("User");
        createdUser.setEnabled(false);
        ContractDTO contractDTO = new ContractDTO();
        createdUser.setContract(contractDTO);
        createdUser.setRole(SPONSOR_ROLE);

        UserDTO updatedUser = userService.updateUser(createdUser);

        Assert.assertEquals(updatedUser.getUsername(), createdUser.getUsername());
        Assert.assertEquals(updatedUser.getEmail(), createdUser.getEmail());
        Assert.assertEquals(updatedUser.getFirstName(), createdUser.getFirstName());
        Assert.assertEquals(updatedUser.getLastName(), createdUser.getLastName());
        Assert.assertEquals(updatedUser.getEnabled(), createdUser.getEnabled());
        Assert.assertEquals(updatedUser.getContract().getContractName(), createdUser.getContract().getContractName());
        Assert.assertEquals(updatedUser.getContract().getContractNumber(), createdUser.getContract().getContractNumber());
        Assert.assertEquals(updatedUser.getRole(), createdUser.getRole());
    }

    @Test
    public void testUpdateUserAddRole() {
        UserDTO user = buildUserDTO("Test", null);

        Assert.assertNull(user.getRole());

        UserDTO createdUser = userService.createUser(user);

        createdUser.setRole(SPONSOR_ROLE);

        UserDTO updatedUser = userService.updateUser(createdUser);

        Assert.assertEquals(updatedUser.getRole(), createdUser.getRole());
    }

    @Test
    public void testUpdateUserRemoveRole() {
        UserDTO user = buildUserDTO("TEST", SPONSOR_ROLE);

        Assert.assertEquals(user.getRole(), SPONSOR_ROLE);

        UserDTO createdUser = userService.createUser(user);

        createdUser.setRole(null);

        UserDTO updatedUser = userService.updateUser(createdUser);

        Assert.assertNull(updatedUser.getRole());
    }

    @Test
    public void testSetupUserAndRolesInSecurityContext() {
        HttpServletRequest httpServletRequest = new MockHttpServletRequest();

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(ADMIN_ROLE));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "ADMIN", null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserDTO user = buildUserDTO("Test", SPONSOR_ROLE);
        UserDTO createdUser = userService.createUser(user);

        String username = createdUser.getUsername();

        userService.setupUserImpersonation(username, httpServletRequest);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Assert.assertEquals(authentication.getPrincipal(), "test@test.com");
        Assert.assertEquals(authentication.getAuthorities().size(), 1);
        GrantedAuthority grantedAuthority = authentication.getAuthorities().iterator().next();
        Assert.assertEquals(grantedAuthority.getAuthority(), SPONSOR_ROLE);
    }

    @Test
    public void testSetupUserAndRolesInSecurityContextBadUser() {
        HttpServletRequest httpServletRequest = new MockHttpServletRequest();
        var exceptionThrown = Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            userService.setupUserImpersonation("UserDoesNotExist", httpServletRequest);
        });
        Assert.assertEquals(exceptionThrown.getMessage(), "User is not present in our database");
    }

    @Test
    public void testEnableUser() {
        UserDTO user = buildUserDTO("Test", SPONSOR_ROLE);
        user.setEnabled(false);

        userService.createUser(user);

        UserDTO updatedUser = userService.enableUser(user.getUsername());
        Assert.assertEquals(updatedUser.getEnabled(), true);
    }

    @Test
    public void testDisableUser() {
        UserDTO user = buildUserDTO("Test", SPONSOR_ROLE);
        userService.createUser(user);

        UserDTO updatedUser = userService.disableUser(user.getUsername());
        Assert.assertEquals(updatedUser.getEnabled(), false);
    }
}
