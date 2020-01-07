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
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.annotation.Nullable;
import javax.validation.ConstraintViolationException;

import static gov.cms.ab2d.common.util.Constants.SPONSOR_ROLE;
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
            userService.createUser(user);
        });
        assertThat(exceptionThrown.getMessage(), is("could not execute statement; SQL [n/a]; constraint [uc_user_account_username]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement"));
    }

    @Test
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
        assertThat(exceptionThrown.getMessage(), is("ModelMapper mapping errors:\n\n1) Converter Converter<class gov.cms.ab2d.common.dto.SponsorDTO, class gov.cms.ab2d.common.model.Sponsor> failed to convert gov.cms.ab2d.common.dto.SponsorDTO to gov.cms.ab2d.common.model.Sponsor.\n\n1 error"));
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

    @Test
    public void testUpdateUserRemoveSponsor() {
        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);

        UserDTO user = createUser(sponsor, SPONSOR_ROLE);

        UserDTO createdUser = userService.createUser(user);

        createdUser.setSponsor(null);

        var exceptionThrown = Assertions.assertThrows(MappingException.class, () -> {
            userService.updateUser(createdUser);
        });
        assertThat(exceptionThrown.getMessage(), is("ModelMapper mapping errors:\n\n1) Converter Converter<class gov.cms.ab2d.common.dto.SponsorDTO, class gov.cms.ab2d.common.model.Sponsor> failed to convert gov.cms.ab2d.common.dto.SponsorDTO to gov.cms.ab2d.common.model.Sponsor.\n\n1 error"
        ));
    }
}
