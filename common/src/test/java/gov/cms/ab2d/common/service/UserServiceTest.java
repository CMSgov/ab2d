package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.dto.SponsorDTO;
import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import gov.cms.ab2d.common.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.MappingException;
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
import javax.validation.ConstraintViolationException;

import java.util.List;

import static gov.cms.ab2d.common.util.Constants.ADMIN_ROLE;
import static gov.cms.ab2d.common.util.Constants.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.DataSetup.TEST_USER;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
@Testcontainers
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private RoleService roleService;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    private DataSetup dataSetup;

    @BeforeEach
    public void init() {
        userRepository.deleteAll();
        sponsorRepository.deleteAll();
    }

    @Test
    public void testUser() {
        User user = userService.getCurrentUser();

        assertEquals(null, user); // no authentication for now, so will be null
    }

    @Test
    public void testCreateUser() {
        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);

        UserDTO user = createUser(sponsor, SPONSOR_ROLE);

        UserDTO createdUser = userService.createUser(user);
        Assert.assertEquals(createdUser.getUsername(), user.getUsername());
        Assert.assertEquals(createdUser.getEmail(), user.getEmail());
        Assert.assertEquals(createdUser.getFirstName(), user.getFirstName());
        Assert.assertEquals(createdUser.getLastName(), user.getLastName());
        Assert.assertEquals(createdUser.getEnabled(), user.getEnabled());
        Assert.assertEquals(createdUser.getSponsor(), user.getSponsor());
        Assert.assertEquals(createdUser.getRole(), SPONSOR_ROLE);
    }

    @Test
    public void testCreateDuplicateUser() {
        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);

        UserDTO user = createUser(sponsor, SPONSOR_ROLE);

        userService.createUser(user);
        var exceptionThrown = Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            user.setEmail("anotherEmail@test.com");
            userService.createUser(user);
        });
        assertThat(exceptionThrown.getMessage(), is("could not execute statement; SQL [n/a]; constraint [uc_user_account_username]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement"));
    }

//    @Test - todo: fix or delete
    public void testCreateDuplicateUserByEmail() {
        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);

        UserDTO user = createUser(sponsor, SPONSOR_ROLE);

        userService.createUser(user);

        user.setUsername("something_different");
        var exceptionThrown = Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            userService.createUser(user);
        });
        assertThat(exceptionThrown.getMessage(), is("could not execute statement; SQL [n/a]; constraint [uc_user_account_email]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement"));
    }

    static private final String EXPECTED_BAD_SPONSOR =
            "ModelMapper mapping errors:" + System.lineSeparator() + System.lineSeparator() +
                    "1) Converter Converter<class gov.cms.ab2d.common.dto.SponsorDTO, " +
                    "class gov.cms.ab2d.common.model.Sponsor> failed to convert " +
                    "gov.cms.ab2d.common.dto.SponsorDTO to gov.cms.ab2d.common.model.Sponsor." +
                    System.lineSeparator() + System.lineSeparator() +"1 error";


    @Test
    public void testCreateUserBadSponsor() {
        UserDTO user = new UserDTO();
        user.setEmail("test@test.com");
        user.setUsername("test@test.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        Role role = roleService.findRoleByName(SPONSOR_ROLE);
        user.setRole(role.getName());

        var exceptionThrown = Assertions.assertThrows(MappingException.class, () -> {
            userService.createUser(user);
        });
        assertEquals(EXPECTED_BAD_SPONSOR, exceptionThrown.getMessage());
    }

    private UserDTO createUser(Sponsor sponsor, @Nullable String roleName) {
        UserDTO user = new UserDTO();
        user.setEmail("test@test.com");
        user.setUsername("test@test.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        SponsorDTO sponsorDTO = new SponsorDTO(sponsor.getHpmsId(), sponsor.getOrgName());
        user.setSponsor(sponsorDTO);
        if(roleName != null) {
            Role role = roleService.findRoleByName(roleName);
            user.setRole(role.getName());
        }

        return user;
    }

    @Test
    public void testUpdateUser() {
        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);

        UserDTO user = createUser(sponsor, SPONSOR_ROLE);

        UserDTO createdUser = userService.createUser(user);

        createdUser.setEmail("newTest@test.com");
        createdUser.setFirstName("New");
        createdUser.setLastName("User");
        createdUser.setEnabled(false);
        SponsorDTO sponsorDTOUpdate = new SponsorDTO(sponsor.getParent().getHpmsId(), sponsor.getParent().getOrgName());
        createdUser.setSponsor(sponsorDTOUpdate);
        createdUser.setRole(SPONSOR_ROLE);

        UserDTO updatedUser = userService.updateUser(createdUser);

        Assert.assertEquals(updatedUser.getUsername(), createdUser.getUsername());
        Assert.assertEquals(updatedUser.getEmail(), createdUser.getEmail());
        Assert.assertEquals(updatedUser.getFirstName(), createdUser.getFirstName());
        Assert.assertEquals(updatedUser.getLastName(), createdUser.getLastName());
        Assert.assertEquals(updatedUser.getEnabled(), createdUser.getEnabled());
        Assert.assertEquals(updatedUser.getSponsor().getOrgName(), createdUser.getSponsor().getOrgName());
        Assert.assertEquals(updatedUser.getSponsor().getHpmsId(), createdUser.getSponsor().getHpmsId());
        Assert.assertEquals(updatedUser.getRole(), createdUser.getRole());
    }

    @Test
    public void testUpdateUserAddRole() {
        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);

        UserDTO user = createUser(sponsor, null);

        Assert.assertNull(user.getRole());

        UserDTO createdUser = userService.createUser(user);

        createdUser.setRole(SPONSOR_ROLE);

        UserDTO updatedUser = userService.updateUser(createdUser);

        Assert.assertEquals(updatedUser.getRole(), createdUser.getRole());
    }

    @Test
    public void testUpdateUserRemoveRole() {
        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);

        UserDTO user = createUser(sponsor, SPONSOR_ROLE);

        Assert.assertEquals(user.getRole(), SPONSOR_ROLE);

        UserDTO createdUser = userService.createUser(user);

        createdUser.setRole(null);

        UserDTO updatedUser = userService.updateUser(createdUser);

        Assert.assertNull(updatedUser.getRole());
    }

    private static final String EXPECTED_REMOVE_MESSAGE =
            "ModelMapper mapping errors:" + System.lineSeparator() + System.lineSeparator() +
            "1) Converter Converter<class gov.cms.ab2d.common.dto.SponsorDTO, class gov.cms.ab2d.common.model.Sponsor> " +
            "failed to convert gov.cms.ab2d.common.dto.SponsorDTO to gov.cms.ab2d.common.model.Sponsor." +
            System.lineSeparator() + System.lineSeparator() + "1 error";

    @Test
    public void testUpdateUserRemoveSponsor() {
        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);

        UserDTO user = createUser(sponsor, SPONSOR_ROLE);

        UserDTO createdUser = userService.createUser(user);

        createdUser.setSponsor(null);

        var exceptionThrown = Assertions.assertThrows(MappingException.class, () -> {
            userService.updateUser(createdUser);
        });
        assertEquals(EXPECTED_REMOVE_MESSAGE, exceptionThrown.getMessage());
    }

    @Test
    public void testSetupUserAndRolesInSecurityContext() {
        HttpServletRequest httpServletRequest = new MockHttpServletRequest();

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(ADMIN_ROLE));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "ADMIN", null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);
        UserDTO user = createUser(sponsor, SPONSOR_ROLE);
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
        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);

        UserDTO user = createUser(sponsor, SPONSOR_ROLE);
        user.setEnabled(false);

        userService.createUser(user);

        UserDTO updatedUser = userService.enableUser(user.getUsername());
        Assert.assertEquals(updatedUser.getEnabled(), true);
    }

    @Test
    public void testDisableUser() {
        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);

        UserDTO user = createUser(sponsor, SPONSOR_ROLE);
        userService.createUser(user);

        UserDTO updatedUser = userService.disableUser(user.getUsername());
        Assert.assertEquals(updatedUser.getEnabled(), false);
    }
}
