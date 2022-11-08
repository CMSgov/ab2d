package gov.cms.ab2d.common.health;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.properties.client.PropertyNotFoundException;
import gov.cms.ab2d.properties.service.PropertiesAPIService;
import gov.cms.ab2d.properties.service.PropertiesAPIServiceImpl;
import gov.cms.ab2d.properties.service.PropertiesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static gov.cms.ab2d.common.util.PropertyConstants.MAINTENANCE_MODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
public class PropertiesServiceAvailableTest {
    private PropertiesServiceAvailable mockPropertiesServiceAvailable;

    @Autowired
    PropertiesService propertiesService;

    @Mock
    private PropertiesAPIService mockPropertiesApiService;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    void init() {
        mockPropertiesServiceAvailable = new PropertiesServiceAvailable(mockPropertiesApiService);
    }

    @AfterEach
    void resetMocks() {
        reset(mockPropertiesApiService);
    }

    @Test
    void testRealPropService() {
        PropertiesAPIService propertiesAPIServiceReal = new PropertiesAPIServiceImpl(false, propertiesService, "http://localhost:5060");
        PropertiesServiceAvailable propertiesServiceAvailableReal = new PropertiesServiceAvailable(propertiesAPIServiceReal);
        assertTrue(propertiesServiceAvailableReal.validBoolean("TRUE"));
        assertTrue(propertiesServiceAvailableReal.validBoolean("true"));
        assertTrue(propertiesServiceAvailableReal.validBoolean("false"));
        assertTrue(propertiesServiceAvailableReal.validBoolean("False"));
        assertFalse(propertiesServiceAvailableReal.validBoolean("1"));
        assertFalse(propertiesServiceAvailableReal.validBoolean("0"));
        assertFalse(propertiesServiceAvailableReal.validBoolean(""));
        assertFalse(propertiesServiceAvailableReal.validBoolean(null));
        assertTrue(propertiesServiceAvailableReal.isAvailable());
    }

    @Test
    void testWithMock() {
        when(mockPropertiesApiService.getProperty("test1")).thenReturn(null);
        assertTrue(mockPropertiesServiceAvailable.checkValue("test1", null));
        assertFalse(mockPropertiesServiceAvailable.checkValue("test1", "one"));

        when(mockPropertiesApiService.getProperty("test2")).thenReturn("two");
        assertTrue(mockPropertiesServiceAvailable.checkValue("test2", "two"));
        assertFalse(mockPropertiesServiceAvailable.checkValue("test2", "one"));

        when(mockPropertiesApiService.getProperty("exception")).thenThrow(PropertyNotFoundException.class);
        assertEquals(null, mockPropertiesServiceAvailable.getValue("exception"));

        when(mockPropertiesApiService.createProperty("test3", "three")).thenReturn(false);
        assertFalse(mockPropertiesServiceAvailable.createValue("test3", "three"));

        when(mockPropertiesApiService.deleteProperty("test4")).thenReturn(false);
        assertFalse(mockPropertiesServiceAvailable.deleteValue("test4"));

        when(mockPropertiesApiService.deleteProperty("test5")).thenReturn(true);
        when(mockPropertiesApiService.getProperty("test5")).thenReturn("five");
        assertFalse(mockPropertiesServiceAvailable.deleteValue("test5"));

        when(mockPropertiesApiService.updateProperty("test6", "six")).thenReturn(false);
        assertFalse(mockPropertiesServiceAvailable.updateValue("test6", "six-six"));

    }

    @Test
    void mainMethod() {
        when(mockPropertiesApiService.getProperty(MAINTENANCE_MODE)).thenReturn(null);
        assertFalse(mockPropertiesServiceAvailable.isAvailable());
    }

    @Test
    void mainMethod2() {
        when(mockPropertiesApiService.getProperty(MAINTENANCE_MODE)).thenReturn("false");
        when(mockPropertiesApiService.createProperty("fake.key", "fake_value")).thenReturn(false);
        assertFalse(mockPropertiesServiceAvailable.isAvailable());
    }
    @Test
    void mainMethod3() {
        when(mockPropertiesApiService.getProperty(MAINTENANCE_MODE)).thenReturn("false");
        when(mockPropertiesApiService.createProperty("fake.key", "fake_value")).thenReturn(true);
        when(mockPropertiesApiService.getProperty("fake.key")).thenReturn("fake_value");
        when(mockPropertiesApiService.updateProperty("fake.key", "fake_value1")).thenReturn(false);
        assertFalse(mockPropertiesServiceAvailable.isAvailable());
    }
    @Test
    void mainMethod4() {
        when(mockPropertiesApiService.getProperty(MAINTENANCE_MODE)).thenReturn("false");
        when(mockPropertiesApiService.createProperty("fake.key", "fake_value")).thenReturn(true);
        when(mockPropertiesApiService.getProperty("fake.key"))
                .thenReturn("fake_value")
                .thenReturn("fake_value1")
                .thenReturn(null);
        when(mockPropertiesApiService.updateProperty("fake.key", "fake_value1")).thenReturn(true);
        when(mockPropertiesApiService.deleteProperty("fake.key")).thenReturn(false);
        assertFalse(mockPropertiesServiceAvailable.isAvailable());
    }
}
