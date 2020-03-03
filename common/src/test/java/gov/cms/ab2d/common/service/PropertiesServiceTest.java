package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.repository.PropertiesRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.Assert;
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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
@Testcontainers
public class PropertiesServiceTest {

    @Autowired
    private PropertiesRepository propertiesRepository;

    @Autowired
    private PropertiesService propertiesService;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Test
    public void testCreationAndRetrieval() {
        Map<String, Object> propertyMap = new HashMap<>(){{
            put("abc", "val");
            put(PCP_CORE_POOL_SIZE, 10);
            put(PCP_MAX_POOL_SIZE, 150);
            put(PCP_SCALE_TO_MAX_TIME, 900);
            put(MAINTENANCE_MODE, "false");
            put(CONTRACT_2_BENE_CACHING_ON, "false");
        }};

        List<Properties> propertyListBeforeInsert = propertiesService.getAllProperties();
        int beforeCount = propertyListBeforeInsert.size();
        Properties properties = new Properties();
        properties.setKey("abc");
        properties.setValue("val");

        propertiesRepository.save(properties);

        List<Properties> propertiesList = propertiesService.getAllProperties();

        Assert.assertEquals(propertiesList.size(), beforeCount + 1);

        for(Properties propertiesToCheck : propertiesList) {
            Object propertyValue = propertyMap.get(propertiesToCheck.getKey());

            Assert.assertNotNull(propertyValue);
            Assert.assertEquals(propertyValue.toString(), propertiesToCheck.getValue());
        }
    }

    @Test
    public void testUpdateProperties() {
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

        List<PropertiesDTO> updatedPropertiesDTOs = propertiesService.updateProperties(propertiesDTOs);

        Assert.assertEquals(4, updatedPropertiesDTOs.size());

        for(PropertiesDTO propertiesDTO : updatedPropertiesDTOs) {
            if(propertiesDTO.getKey().equals(PCP_CORE_POOL_SIZE)) {
                Assert.assertEquals("15", propertiesDTO.getValue());
            } else if (propertiesDTO.getKey().equals(PCP_MAX_POOL_SIZE)) {
                Assert.assertEquals("350", propertiesDTO.getValue());
            } else if (propertiesDTO.getKey().equals(PCP_SCALE_TO_MAX_TIME)) {
                Assert.assertEquals("400", propertiesDTO.getValue());
            } else if (propertiesDTO.getKey().equals(MAINTENANCE_MODE)) {
                Assert.assertEquals("true", propertiesDTO.getValue());
            } else {
                Assert.fail("Received unknown key");
            }
        }

        // Cleanup
        propertiesDTOs.clear();
        propertiesDTOMaintenanceMode.setValue("false");
        propertiesDTOs.add(propertiesDTOMaintenanceMode);
        propertiesService.updateProperties(propertiesDTOs);
    }

    private void validateInvalidPropertyValues(String key, String value) {
        List<PropertiesDTO> propertiesDTOs = new ArrayList<>();
        PropertiesDTO propertiesDTO = new PropertiesDTO();
        propertiesDTO.setKey(key);
        propertiesDTO.setValue(value);
        propertiesDTOs.add(propertiesDTO);

        var exceptionThrown = assertThrows(InvalidPropertiesException.class,
                () -> propertiesService.updateProperties(propertiesDTOs));

        assertThat(exceptionThrown.getMessage(), equalTo(String.format("Incorrect value for %s of %s", key, value)));
    }

    @Test
    public void testUpdatePropertiesInvalidValues() {
        var invalidKeysValues = new HashMap<String, String>(){{
            put(PCP_CORE_POOL_SIZE, "101");
            put(PCP_CORE_POOL_SIZE, "0");
            put(PCP_MAX_POOL_SIZE, "501");
            put(PCP_MAX_POOL_SIZE, "0");
            put(PCP_SCALE_TO_MAX_TIME, "3601");
            put(PCP_SCALE_TO_MAX_TIME, "0");
            put(MAINTENANCE_MODE, "BADVALUE");
        }};

        invalidKeysValues.forEach((key, value) -> {
            validateInvalidPropertyValues(key, value);
        });
    }

    @Test
    public void testUpdatePropertiesInvalidKey() {
        List<PropertiesDTO> propertiesDTOs = new ArrayList<>();
        PropertiesDTO propertiesDTO = new PropertiesDTO();
        propertiesDTO.setKey("Bad Name");
        propertiesDTO.setValue("101");
        propertiesDTOs.add(propertiesDTO);

        var exceptionThrown = assertThrows(InvalidPropertiesException.class,
                () -> propertiesService.updateProperties(propertiesDTOs));

        assertThat(exceptionThrown.getMessage(), equalTo("Properties must contain a valid key name, received Bad Name"));
    }

    @Test
    public void testGetPropertiesBadKey() {
        var exceptionThrown = assertThrows(ResourceNotFoundException.class,
                () -> propertiesService.getPropertiesByKey("badKey"));

        assertThat(exceptionThrown.getMessage(), equalTo("No entry was found for key badKey"));
    }
}
