package gov.cms.ab2d.properties.service;

import gov.cms.ab2d.properties.SpringBootApp;
import gov.cms.ab2d.properties.dto.PropertiesDTO;
import gov.cms.ab2d.properties.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
public class PropertiesAPIServiceTest {
    private static String MAINT_MODE = "maintenance.mode";
    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer("data-postgres.sql");

    @Autowired
    private PropertiesAPIService apiService;

    @AfterEach
    void reset() {
        assertTrue(apiService.updateProperty("pcp.scaleToMax.time", "800"));
    }

    @Test
    void testGetPropertyNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> apiService.getProperty("nonsense"));
    }

    @Test
    void testGetProperty() {
        String value = apiService.getProperty("pcp.scaleToMax.time");
        assertNotNull(value);
        assertEquals("800", value);
    }

    @Test
    void testGetAllProperties() {
        List<PropertiesDTO> allProperties = apiService.getAllProperties();
        assertNotNull(allProperties);
        assertTrue(allProperties.size() > 0);
    }

    @Test
    void testInMaintMode() {
        String prevValue = apiService.getProperty(MAINT_MODE);
        Boolean boolVal = Boolean.parseBoolean(prevValue);
        Boolean newVal = !boolVal;
        apiService.updateProperty(MAINT_MODE, "" + newVal);
        apiService.updateProperty(MAINT_MODE, prevValue);
        assertEquals(prevValue, "" + apiService.isToggleOn(MAINT_MODE));
    }

    @Test
    void testCreateNew() {
        String newPropName = "new_one";
        String newPropVal = "new_val";

        assertFalse(apiService.createProperty(null, newPropVal));
        assertThrows(ResourceNotFoundException.class, () -> apiService.getProperty(newPropName));
        Assertions.assertTrue(apiService.createProperty(newPropName, newPropVal));
        assertFalse(apiService.createProperty(newPropName, newPropVal));
        assertEquals(newPropVal, apiService.getProperty(newPropName));
        Assertions.assertTrue(apiService.deleteProperty(newPropName));
        assertThrows(ResourceNotFoundException.class, () -> apiService.getProperty(newPropName));
        assertThrows(ResourceNotFoundException.class, () -> apiService.deleteProperty("DOESNT EXIST"));
        assertFalse(apiService.deleteProperty(null));
    }

}
