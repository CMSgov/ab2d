package gov.cms.ab2d.common.model;

import gov.cms.ab2d.common.repository.RoleRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SpringBootTest
@Testcontainers
@TestPropertySource(locations = "/application.common.properties")
public class CreateUpdateTimestampTest {

    private static final String ROLE_NAME = "Timestamp Test Role";
    private static final String ROLE_NAME_TOO = "Timestamp Test Role 2";


    @Autowired
    RoleRepository roleRepository;

    @SuppressWarnings("rawtypes")
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Test
    public void testTimestamps() {
        Role role = new Role();
        role.setName(ROLE_NAME);
        assertNull(role.getId());
        assertNull(role.getCreated());
        assertNull(role.getModified());

        Role savedRole = roleRepository.save(role);
        assertEquals(ROLE_NAME, savedRole.getName());
        assertNotNull(savedRole.getId());
        assertNotNull(savedRole.getCreated());
        assertNotNull(savedRole.getModified());

        LocalDateTime created = savedRole.getCreated();
        LocalDateTime modified = savedRole.getModified();
        savedRole.setName(ROLE_NAME_TOO);
        Role finaleRole = roleRepository.save(savedRole);
        assertEquals(created, finaleRole.getCreated());
        assertNotEquals(modified, finaleRole.getModified());
    }

    @AfterEach
    private void tearDown() {
        roleRepository.deleteAll();
    }
}
