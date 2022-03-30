package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.repository.PropertiesRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.cms.ab2d.common.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
@Testcontainers
class PropertiesServiceTest {

    @Autowired
    private PropertiesRepository propertiesRepository;

    @Autowired
    private PropertiesService propertiesService;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Test
    void testCreationAndRetrieval() {
        Map<String, Object> propertyMap = new HashMap<>(){{
            put("abc", "val");
            put(PCP_CORE_POOL_SIZE, 10);
            put(PCP_MAX_POOL_SIZE, 150);
            put(PCP_SCALE_TO_MAX_TIME, 900);
            put(MAINTENANCE_MODE, "false");
            put(ZIP_SUPPORT_ON, "false");
            put(WORKER_ENGAGEMENT, "engaged");
            put(HPMS_INGESTION_ENGAGEMENT, "engaged");
            put(COVERAGE_SEARCH_DISCOVERY, "idle");
            put(COVERAGE_SEARCH_QUEUEING, "idle");
            put(COVERAGE_SEARCH_UPDATE_MONTHS, "1");
            put(COVERAGE_SEARCH_STUCK_HOURS, "24");
            put(COVERAGE_SEARCH_OVERRIDE, "false");
            put(MAX_DOWNLOADS, "6");
            put(RE_DOWNLOAD_MAX_INTERVAL_MINUTES, "30");
        }};

        List<Properties> propertyListBeforeInsert = propertiesService.getAllProperties();
        int beforeCount = propertyListBeforeInsert.size();

        Properties properties = new Properties();
        properties.setKey("abc");
        properties.setValue("val");

        propertiesRepository.save(properties);

        List<Properties> propertiesList = propertiesService.getAllProperties();

        assertEquals(propertiesList.size(), beforeCount + 1);

        for(Properties propertiesToCheck : propertiesList) {
            Object propertyValue = propertyMap.get(propertiesToCheck.getKey());

            assertNotNull(propertyValue);
            assertEquals(propertyValue.toString(), propertiesToCheck.getValue());
        }
    }

    @Test
    void testMisc() {
        PropertiesDTO val = new PropertiesDTO();
        val.setKey(MAINTENANCE_MODE);
        assertFalse(propertiesService.isToggleOn(MAINTENANCE_MODE));
        List<PropertiesDTO> currentProperties = propertiesService.getAllPropertiesDTO();
        currentProperties.stream().filter(c -> c.getKey().equalsIgnoreCase(MAINTENANCE_MODE)).findFirst().get().setValue("true");
        propertiesService.updateProperties(currentProperties);
        assertTrue(propertiesService.isToggleOn(MAINTENANCE_MODE));
        assertTrue(propertiesService.isInMaintenanceMode());
        List<PropertiesDTO> vals = propertiesService.getAllPropertiesDTO();
        assertEquals("true", (vals.stream()
                .filter(c -> c.getKey().equalsIgnoreCase(MAINTENANCE_MODE)).findFirst().map(PropertiesDTO::getValue).get()));
        currentProperties.stream().filter(c -> c.getKey().equalsIgnoreCase(MAINTENANCE_MODE)).findFirst().get().setValue("false");
        propertiesService.updateProperties(currentProperties);
    }

    @Test
    void testUpdateProperties() {
        List<PropertiesDTO> propertiesDTOs = new ArrayList<>();
        PropertiesDTO propertiesDTOPoolSize = new PropertiesDTO();
        propertiesDTOPoolSize.setKey(PCP_CORE_POOL_SIZE);
        propertiesDTOPoolSize.setValue("15");
        propertiesDTOs.add(propertiesDTOPoolSize);

        PropertiesDTO propertiesDTOMaxPoolSize = new PropertiesDTO();
        propertiesDTOMaxPoolSize.setKey(PCP_MAX_POOL_SIZE);
        propertiesDTOMaxPoolSize.setValue("350");
        propertiesDTOs.add(propertiesDTOMaxPoolSize);

        PropertiesDTO propertiesDTOScaleToMaxTime = new PropertiesDTO();
        propertiesDTOScaleToMaxTime.setKey(PCP_SCALE_TO_MAX_TIME);
        propertiesDTOScaleToMaxTime.setValue("400");
        propertiesDTOs.add(propertiesDTOScaleToMaxTime);

        PropertiesDTO propertiesDTOMaintenanceMode = new PropertiesDTO();
        propertiesDTOMaintenanceMode.setKey(MAINTENANCE_MODE);
        propertiesDTOMaintenanceMode.setValue("true");
        propertiesDTOs.add(propertiesDTOMaintenanceMode);

        PropertiesDTO propertiesDTOZipSupportOn = new PropertiesDTO();
        propertiesDTOZipSupportOn.setKey(ZIP_SUPPORT_ON);
        propertiesDTOZipSupportOn.setValue("true");
        propertiesDTOs.add(propertiesDTOZipSupportOn);

        List<PropertiesDTO> updatedPropertiesDTOs = propertiesService.updateProperties(propertiesDTOs);

        assertEquals(5, updatedPropertiesDTOs.size());

        for(PropertiesDTO propertiesDTO : updatedPropertiesDTOs) {
            switch (propertiesDTO.getKey()) {
                case PCP_CORE_POOL_SIZE:
                    assertEquals("15", propertiesDTO.getValue());
                    break;
                case PCP_MAX_POOL_SIZE:
                    assertEquals("350", propertiesDTO.getValue());
                    break;
                case PCP_SCALE_TO_MAX_TIME:
                    assertEquals("400", propertiesDTO.getValue());
                    break;
                case MAINTENANCE_MODE:
                case ZIP_SUPPORT_ON:
                    assertEquals("true", propertiesDTO.getValue());
                    break;
                default:
                    fail("Received unknown key");
                    break;
            }
        }

        // Cleanup
        propertiesDTOs.clear();
        propertiesDTOMaintenanceMode.setValue("false");
        propertiesDTOs.add(propertiesDTOMaintenanceMode);
        propertiesService.updateProperties(propertiesDTOs);
    }

    @Test
    void testValidProperties() {
        PropertiesDTO p = new PropertiesDTO();
        p.setKey("Bad");
        p.setValue("Value");
        PropertiesServiceImpl val = (PropertiesServiceImpl) propertiesService;
        assertThrows(InvalidPropertiesException.class, () -> val.checkNameOfPropertyKey(p));

        PropertiesDTO p2 = new PropertiesDTO();
        p2.setKey(PCP_CORE_POOL_SIZE);
        p2.setValue("200");
        assertThrows(InvalidPropertiesException.class, () -> val.validateInt(PCP_CORE_POOL_SIZE, p2, 1, 100));
        p2.setValue("100");
        val.validateInt(PCP_CORE_POOL_SIZE, p2, 1, 100);

        p2.setKey(PCP_CORE_POOL_SIZE);
        p2.setValue("");
        assertThrows(InvalidPropertiesException.class, () -> val.validateBoolean(ZIP_SUPPORT_ON, p2));
        p2.setValue("true");
        val.validateBoolean(ZIP_SUPPORT_ON, p2);
        p2.setValue("false");
        val.validateBoolean(ZIP_SUPPORT_ON, p2);
        p2.setValue(null);
        assertThrows(NullPointerException.class, () -> val.validateBoolean(ZIP_SUPPORT_ON, p2));
    }

    private void validateInvalidPropertyValues(String key, String value) {
        List<PropertiesDTO> propertiesDTOs = new ArrayList<>();
        PropertiesDTO propertiesDTO = new PropertiesDTO();
        propertiesDTO.setKey(key);
        propertiesDTO.setValue(value);
        propertiesDTOs.add(propertiesDTO);

        var exceptionThrown = assertThrows(InvalidPropertiesException.class,
                () -> propertiesService.updateProperties(propertiesDTOs));

        assertEquals(String.format("Incorrect value for %s of %s", key, value), exceptionThrown.getMessage());
    }

    @Test
    void testUpdatePropertiesInvalidValues() {
        var invalidKeysValues = new HashMap<String, String>(){{
            put(PCP_CORE_POOL_SIZE, "101");
            put(PCP_CORE_POOL_SIZE, "0");
            put(PCP_MAX_POOL_SIZE, "501");
            put(PCP_MAX_POOL_SIZE, "0");
            put(PCP_SCALE_TO_MAX_TIME, "3601");
            put(PCP_SCALE_TO_MAX_TIME, "0");
            put(MAINTENANCE_MODE, "BADVALUE");
        }};

        invalidKeysValues.forEach(this::validateInvalidPropertyValues);
    }

    @Test
    void testUpdatePropertiesInvalidKey() {
        List<PropertiesDTO> propertiesDTOs = new ArrayList<>();
        PropertiesDTO propertiesDTO = new PropertiesDTO();
        propertiesDTO.setKey("Bad Name");
        propertiesDTO.setValue("101");
        propertiesDTOs.add(propertiesDTO);

        var exceptionThrown = assertThrows(InvalidPropertiesException.class,
                () -> propertiesService.updateProperties(propertiesDTOs));

        assertEquals("Properties must contain a valid key name, received Bad Name", exceptionThrown.getMessage());
    }

    @Test
    void testGetPropertiesBadKey() {
        var exceptionThrown = assertThrows(ResourceNotFoundException.class,
                () -> propertiesService.getPropertiesByKey("badKey"));

        assertEquals("No entry was found for key badKey", exceptionThrown.getMessage());
    }
}
