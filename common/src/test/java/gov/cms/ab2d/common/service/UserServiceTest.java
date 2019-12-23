package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.config.Mapping;
import gov.cms.ab2d.common.dto.SponsorDTO;
import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
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

import static gov.cms.ab2d.common.util.Constants.ADMIN_ROLE;
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
    private RoleService roleService;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private Mapping mapping;

    @BeforeEach
    public void init() {
        userRepository.deleteAll();
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

        User createdUser = userService.createUser(user);
        Assert.assertEquals(createdUser.getUsername(), user.getUsername());
        Assert.assertEquals(createdUser.getEmail(), user.getEmail());
        Assert.assertEquals(createdUser.getFirstName(), user.getFirstName());
        Assert.assertEquals(createdUser.getLastName(), user.getLastName());
        Assert.assertEquals(createdUser.getEnabled(), user.getEnabled());
        Assert.assertEquals(createdUser.getSponsor().getId(), sponsor.getId());
        Assert.assertEquals(createdUser.getRoles().iterator().next().getName(), SPONSOR_ROLE);
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

        var exceptionThrown = Assertions.assertThrows(ConstraintViolationException.class, () -> {
            userService.createUser(user);
        });
        assertThat(exceptionThrown.getMessage(), is("Validation failed for classes [gov.cms.ab2d.common.model.User] during persist time for groups [javax.validation.groups.Default, ]\nList of constraint violations:[\n\tConstraintViolationImpl{interpolatedMessage='must not be null', propertyPath=sponsor, rootBeanClass=class gov.cms.ab2d.common.model.User, messageTemplate='{javax.validation.constraints.NotNull.message}'}\n]"));
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

        User createdUser = userService.createUser(user);

        UserDTO createdUserDTO = mapping.getModelMapper().map(createdUser, UserDTO.class);

        createdUserDTO.setEmail("newTest@test.com");
        createdUserDTO.setFirstName("New");
        createdUserDTO.setLastName("User");
        createdUserDTO.setEnabled(false);
        SponsorDTO sponsorDTOUpdate = new SponsorDTO(sponsor.getParent().getHpmsId(), sponsor.getParent().getOrgName());
        createdUserDTO.setSponsor(sponsorDTOUpdate);
        createdUserDTO.setRole(SPONSOR_ROLE);

        User updatedUser = userService.updateUser(createdUserDTO);

        UserDTO updatedUserDTO = mapping.getModelMapper().map(updatedUser, UserDTO.class);

        Assert.assertEquals(updatedUserDTO.getUsername(), createdUserDTO.getUsername());
        Assert.assertEquals(updatedUserDTO.getEmail(), createdUserDTO.getEmail());
        Assert.assertEquals(updatedUserDTO.getFirstName(), createdUserDTO.getFirstName());
        Assert.assertEquals(updatedUserDTO.getLastName(), createdUserDTO.getLastName());
        Assert.assertEquals(updatedUserDTO.getEnabled(), createdUserDTO.getEnabled());
        Assert.assertEquals(updatedUserDTO.getSponsor().getOrgName(), createdUserDTO.getSponsor().getOrgName());
        Assert.assertEquals(updatedUserDTO.getSponsor().getHpmsId(), createdUserDTO.getSponsor().getHpmsId());
        Assert.assertEquals(updatedUserDTO.getRole(), createdUserDTO.getRole());
    }

    @Test
    public void testUpdateUserAddRole() {
        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);

        UserDTO user = createUser(sponsor, null);

        Assert.assertNull(user.getRole());

        User createdUser = userService.createUser(user);

        UserDTO createdUserDTO = mapping.getModelMapper().map(createdUser, UserDTO.class);

        createdUserDTO.setRole(SPONSOR_ROLE);

        User updatedUser = userService.updateUser(createdUserDTO);

        UserDTO updatedUserDTO = mapping.getModelMapper().map(updatedUser, UserDTO.class);

        Assert.assertEquals(updatedUserDTO.getRole(), createdUserDTO.getRole());
    }

    @Test
    public void testUpdateUserRemoveRole() {
        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);

        UserDTO user = createUser(sponsor, SPONSOR_ROLE);

        Assert.assertEquals(user.getRole(), SPONSOR_ROLE);

        User createdUser = userService.createUser(user);

        UserDTO createdUserDTO = mapping.getModelMapper().map(createdUser, UserDTO.class);

        createdUserDTO.setRole(null);

        User updatedUser = userService.updateUser(createdUserDTO);

        UserDTO updatedUserDTO = mapping.getModelMapper().map(updatedUser, UserDTO.class);

        Assert.assertNull(updatedUserDTO.getRole());
    }

    @Test
    public void testUpdateUserRemoveSponsor() {
        Sponsor sponsor = dataSetup.createSponsor("Parent Corp.", 456, "Test", 123);

        UserDTO user = createUser(sponsor, SPONSOR_ROLE);

        User createdUser = userService.createUser(user);

        UserDTO createdUserDTO = mapping.getModelMapper().map(createdUser, UserDTO.class);

        createdUserDTO.setSponsor(null);

        var exceptionThrown = Assertions.assertThrows(MappingException.class, () -> {
            userService.updateUser(createdUserDTO);
        });
        assertThat(exceptionThrown.getMessage(), is("ModelMapper mapping errors:\n\n1) Converter Converter<class gov.cms.ab2d.common.dto.SponsorDTO, class gov.cms.ab2d.common.model.Sponsor> failed to convert gov.cms.ab2d.common.dto.SponsorDTO to gov.cms.ab2d.common.model.Sponsor.\n\n1 error"
));
    }
}
