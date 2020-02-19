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
        }};

        Properties properties = new Properties();
        properties.setKey("abc");
        properties.setValue("val");

        propertiesRepository.save(properties);

        List<Properties> propertiesList = propertiesService.getAllProperties();

        Assert.assertEquals(propertiesList.size(), 4);

        for(Properties propertiesToCheck : propertiesList) {
            Object propertyValue = propertyMap.get(propertiesToCheck.getKey());

            Assert.assertNotNull(propertyValue);
            Assert.assertEquals(propertyValue.toString(), propertiesToCheck.getValue());
        }
    }

    @Test
    public void testUpdateProperties() {
        List<PropertiesDTO> propertiesDTOs = new ArrayList<>();
        PropertiesDTO propertiesDTO

        List<PropertiesDTO> updatedPropertiesDTOs = propertiesService.updateProperties(propertiesDTOs);
    }
}
