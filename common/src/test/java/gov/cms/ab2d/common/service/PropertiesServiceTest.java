package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
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

import java.util.List;

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
        Properties properties = new Properties();
        properties.setKey("abc");
        properties.setValue("val");

        Properties retrievedProperties = propertiesRepository.save(properties);

        List<Properties> propertiesList = propertiesService.getAllProperties();
        Properties propertiesFromList = propertiesList.get(0);

        Assert.assertEquals(retrievedProperties.getValue(), propertiesFromList.getValue());
        Assert.assertEquals(retrievedProperties.getKey(), propertiesFromList.getKey());
    }
}
