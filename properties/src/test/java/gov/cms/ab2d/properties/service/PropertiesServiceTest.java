package gov.cms.ab2d.properties.service;

import gov.cms.ab2d.properties.SpringBootApp;
import gov.cms.ab2d.properties.dto.PropertiesDTO;
import gov.cms.ab2d.properties.model.Properties;
import gov.cms.ab2d.properties.repository.PropertiesRepository;
import gov.cms.ab2d.properties.utils.AB2DPostgresqlContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
class PropertiesServiceTest {
    private static String MAINT_MODE = "maintenance.mode";

    @Autowired
    private PropertiesRepository propertiesRepository;

    @Autowired
    private PropertiesService propertiesService;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer("data-postgres.sql");

    private static final String BOGUS_PARAMETER = "abc";

    @Test
    void testCreationAndRetrieval() {
        Map<String, Object> propertyMap = new HashMap<>(){{
            put(BOGUS_PARAMETER, "val");
            put("pcp.core.pool.size", 10);
            put("pcp.max.pool.size", 150);
            put("pcp.scaleToMax.time", 800);
            put(MAINT_MODE, "false");
            put("ZipSupportOn", "false");
            put("worker.engaged", "engaged");
            put("hpms.ingest.engaged", "engaged");
            put("coverage.update.discovery", "idle");
            put("coverage.update.queueing", "idle");
            put("coverage.update.months.past", "1");
            put("coverage.update.stuck.hours", "24");
            put("coverage.update.override", "false");
        }};

        List<Properties> propertyListBeforeInsert = propertiesService.getAllProperties();
        int beforeCount = propertyListBeforeInsert.size();

        Properties properties = new Properties();
        properties.setKey(BOGUS_PARAMETER);
        properties.setValue("val");

        propertiesRepository.save(properties);
        Properties newprop = propertiesService.getPropertiesByKey(BOGUS_PARAMETER);
        assertNotNull(newprop.getCreated());
        assertNotNull(newprop.getModified());
        assertNotNull(newprop.getKey());
        assertNotNull(newprop.getValue());
        assertTrue(newprop.getId() != 0);

        List<Properties> propertiesList = propertiesService.getAllProperties();

        assertEquals(propertiesList.size(), beforeCount + 1);

        for (Properties propertiesToCheck : propertiesList) {
            Object propertyValue = propertyMap.get(propertiesToCheck.getKey());

            assertNotNull(propertyValue);
            Assertions.assertEquals(propertyValue.toString(), propertiesToCheck.getValue());
        }

        Optional<Properties> prop = propertiesRepository.findByKey(BOGUS_PARAMETER);
        prop.ifPresent(value -> propertiesRepository.delete(value));
    }

    @Test
    void testMisc() {
        assertFalse(propertiesService.isToggleOn(MAINT_MODE));
        List<PropertiesDTO> currentProperties = propertiesService.getAllPropertiesDTO();
        currentProperties.stream().filter(c -> c.getKey().equalsIgnoreCase(MAINT_MODE)).findFirst().get().setValue("true");
        currentProperties.forEach(p -> propertiesService.updateProperty(p));
        assertTrue(propertiesService.isToggleOn(MAINT_MODE));
        List<PropertiesDTO> vals = propertiesService.getAllPropertiesDTO();
        assertEquals("true", (vals.stream()
                .filter(c -> c.getKey().equalsIgnoreCase(MAINT_MODE)).findFirst().map(PropertiesDTO::getValue).get()));
        currentProperties.stream().filter(c -> c.getKey().equalsIgnoreCase(MAINT_MODE)).findFirst().get().setValue("false");
        currentProperties.forEach(p -> propertiesService.updateProperty(p));
    }

    @Test
    void testUpdateProperties() {
        String propKey = "pcp.core.pool.size";
        String origValue = propertiesService.getPropertiesByKey(propKey).getValue();
        PropertiesDTO propertiesDTOPoolSize = new PropertiesDTO(propKey, "15");
        propertiesService.updateProperty(propertiesDTOPoolSize);
        assertEquals("15", propertiesService.getPropertiesByKey(propKey).getValue());
        propertiesDTOPoolSize.setValue(origValue);
        propertiesService.updateProperty(propertiesDTOPoolSize);
    }


    @Test
    void testUpdatePropertiesInvalidKey() {
        PropertiesDTO propertiesDTO = new PropertiesDTO();
        propertiesDTO.setKey("Bad Name");
        propertiesDTO.setValue("101");

        var exceptionThrown = assertThrows(InvalidPropertiesException.class,
                () -> propertiesService.updateProperty(propertiesDTO));

        assertEquals("Properties must contain a valid key name, received Bad Name", exceptionThrown.getMessage());
    }

    @Test
    void testGetPropertiesBadKey() {
        var exceptionThrown = assertThrows(ResourceNotFoundException.class,
                () -> propertiesService.getPropertiesByKey("badKey"));

        assertEquals("No entry was found for key badKey", exceptionThrown.getMessage());
    }

    @Test
    void testCreateNew() {
        String newPropName = "new_one";
        String newPropVal = "new_val";

        assertFalse(propertiesService.insertProperty(null, newPropVal));
        assertThrows(ResourceNotFoundException.class, () -> propertiesService.getPropertiesByKey(newPropName));
        assertTrue(propertiesService.insertProperty(newPropName, newPropVal));
        assertFalse(propertiesService.insertProperty(newPropName, newPropVal));
        assertEquals(newPropVal, propertiesService.getPropertiesByKey(newPropName).getValue());
        assertTrue(propertiesService.deleteProperty(newPropName));
        assertThrows(ResourceNotFoundException.class, () -> propertiesService.getPropertiesByKey(newPropName));
    }

    @Test
    void testDelete() {
        assertFalse(propertiesService.deleteProperty(null));
        assertFalse(propertiesService.deleteProperty(""));
        assertThrows(ResourceNotFoundException.class, () -> assertFalse(propertiesService.deleteProperty("DOESNT_EXIST")));
    }
}
