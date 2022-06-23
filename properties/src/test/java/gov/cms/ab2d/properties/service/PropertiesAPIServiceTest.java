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

import static gov.cms.ab2d.properties.util.Constants.MAINTENANCE_MODE;
import static gov.cms.ab2d.properties.util.Constants.PCP_SCALE_TO_MAX_TIME;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
public class PropertiesAPIServiceTest {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer("data-postgres.sql");

    @Autowired
    private PropertiesAPIService apiService;

    @AfterEach
    void reset() {
        assertTrue(apiService.updateProperty(PCP_SCALE_TO_MAX_TIME, "800"));
    }

    @Test
    void testGetPropertyNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> apiService.getProperty("nonsense"));
    }

    @Test
    void testGetProperty() {
        String value = apiService.getProperty(PCP_SCALE_TO_MAX_TIME);
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
        String prevValue = apiService.getProperty(MAINTENANCE_MODE);
        Boolean boolVal = Boolean.parseBoolean(prevValue);
        assertEquals(boolVal, apiService.isInMaintenanceMode());
        Boolean newVal = !boolVal;
        PropertiesDTO propertiesDTO = new PropertiesDTO(MAINTENANCE_MODE, "" + newVal);
        apiService.updateProperties(List.of(propertiesDTO));
        assertEquals(newVal, apiService.isInMaintenanceMode());
        apiService.updateProperty(MAINTENANCE_MODE, prevValue);
        assertEquals(prevValue, "" + apiService.isToggleOn(MAINTENANCE_MODE));
        assertEquals(prevValue, "" + apiService.isInMaintenanceMode());
    }

    @Test
    void testUpdateProperty() {
        assertTrue(apiService.updateProperty(PCP_SCALE_TO_MAX_TIME, "400"));
        assertThrows(InvalidPropertiesException.class, () -> apiService.updateProperty(MAINTENANCE_MODE, "400"));
        String value = apiService.getProperty(PCP_SCALE_TO_MAX_TIME);
        assertEquals("400", value);
        assertThrows(InvalidPropertiesException.class, () -> apiService.updateProperty("BOGUS", "true"));
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
