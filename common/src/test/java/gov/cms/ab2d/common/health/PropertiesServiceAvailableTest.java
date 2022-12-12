package gov.cms.ab2d.common.health;

import gov.cms.ab2d.common.properties.PropertyServiceStub;
import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.properties.client.PropertyNotFoundException;
import gov.cms.ab2d.common.properties.PropertiesServiceImpl;
import gov.cms.ab2d.common.properties.PropertiesService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
class PropertiesServiceAvailableTest {
    private PropertiesServiceAvailable mockPropertiesServiceAvailable;

    @Autowired
    PropertiesService propertiesService;

    @Mock
    private PropertiesService mockPropertiesService;

    private PropertiesService noDeletePropertiesService = new PropertyServiceStub() {
        @Override
        public boolean deleteProperty(String key) {
            return false;
        }
    };

    private PropertiesService noUpdatePropertiesService = new PropertyServiceStub() {
        @Override
        public boolean updateProperty(String property, String value) {
            return false;
        }
    };

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    void init() {
        mockPropertiesServiceAvailable = new PropertiesServiceAvailable(mockPropertiesService);
    }

    @AfterEach
    void resetMocks() {
        reset(mockPropertiesService);
    }

    @Test
    void testRealPropService() {
        PropertiesService propertiesServiceReal = new PropertiesServiceImpl("http://localhost:5060");
        PropertiesServiceAvailable propertiesServiceAvailableReal = new PropertiesServiceAvailable(propertiesServiceReal);
        assertTrue(propertiesServiceAvailableReal.validBoolean("TRUE"));
        assertTrue(propertiesServiceAvailableReal.validBoolean("true"));
        assertTrue(propertiesServiceAvailableReal.validBoolean("false"));
        assertTrue(propertiesServiceAvailableReal.validBoolean("False"));
        assertFalse(propertiesServiceAvailableReal.validBoolean("1"));
        assertFalse(propertiesServiceAvailableReal.validBoolean("0"));
        assertFalse(propertiesServiceAvailableReal.validBoolean(""));
        assertFalse(propertiesServiceAvailableReal.validBoolean(null));
        assertTrue(propertiesServiceAvailableReal.isAvailable(false));
        assertTrue(propertiesServiceAvailableReal.isAvailable(false));
    }

    @Test
    void testWithMock() {
        when(mockPropertiesService.getProperty(eq("test1"), anyString())).thenReturn(null);
        assertTrue(mockPropertiesServiceAvailable.checkValue("test1", null));
        assertFalse(mockPropertiesServiceAvailable.checkValue("test1", "one"));

        when(mockPropertiesService.getProperty(eq("test2"), anyString())).thenReturn("two");
        assertTrue(mockPropertiesServiceAvailable.checkValue("test2", "two"));
        assertFalse(mockPropertiesServiceAvailable.checkValue("test2", "one"));

        when(mockPropertiesService.getProperty(eq("exception"), anyString())).thenThrow(PropertyNotFoundException.class);
        assertEquals(null, mockPropertiesServiceAvailable.getValue("exception", anyString()));

        when(mockPropertiesService.createProperty("test3", "three")).thenReturn(false);
        assertFalse(mockPropertiesServiceAvailable.createValue("test3", "three"));

        when(mockPropertiesService.deleteProperty("test4")).thenReturn(false);
        assertFalse(mockPropertiesServiceAvailable.deleteValue("test4"));

        when(mockPropertiesService.deleteProperty("test5")).thenReturn(true);
        when(mockPropertiesService.getProperty(eq("test5"), anyString())).thenReturn("five");

        when(mockPropertiesService.updateProperty("test6", "six")).thenReturn(false);
        assertFalse(mockPropertiesServiceAvailable.updateValue("test6", "six-six"));
    }

    @Test
    void testWithMockAgain() {
        when(mockPropertiesService.deleteProperty("test5")).thenReturn(true);
        when(mockPropertiesService.getProperty("test5", null)).thenReturn("five");
        assertFalse(mockPropertiesServiceAvailable.deleteValue("test5"));
    }

    @Test
    void testMockException() {
        when(mockPropertiesService.deleteProperty("test5")).thenReturn(true);
        when(mockPropertiesService.getProperty("test5", null)).thenThrow(RuntimeException.class);
        assertTrue(mockPropertiesServiceAvailable.deleteValue("test5"));
    }

    @Test
    void mainMethod() {
        when(mockPropertiesService.getProperty(eq(MAINTENANCE_MODE), anyString())).thenReturn("five");
        assertFalse(mockPropertiesServiceAvailable.isAvailable(true));
    }

    @Test
    void mainMethod2() {
        when(mockPropertiesService.getProperty(eq(MAINTENANCE_MODE), anyString())).thenReturn("false");
        when(mockPropertiesService.createProperty("fake.key", "fake_value")).thenReturn(false);
        assertFalse(mockPropertiesServiceAvailable.isAvailable(true));
    }
    @Test
    void mainMethod3() {
        when(mockPropertiesService.getProperty(eq(MAINTENANCE_MODE), anyString())).thenReturn("false");
        when(mockPropertiesService.createProperty("fake.key", "fake_value")).thenReturn(true);
        when(mockPropertiesService.getProperty(eq("fake.key"), anyString())).thenReturn("fake_value");
        when(mockPropertiesService.updateProperty("fake.key", "fake_value1")).thenReturn(false);
        assertTrue(mockPropertiesServiceAvailable.isAvailable(false));
    }
    @Test
    void mainMethod4() {
        when(mockPropertiesService.getProperty(eq(MAINTENANCE_MODE), anyString())).thenReturn("false");
        when(mockPropertiesService.createProperty("fake.key", "fake_value")).thenReturn(true);
        when(mockPropertiesService.getProperty(eq("fake.key"), anyString()))
                .thenReturn("fake_value")
                .thenReturn("fake_value1")
                .thenReturn(null);
        when(mockPropertiesService.updateProperty("fake.key", "fake_value1")).thenReturn(true);
        when(mockPropertiesService.deleteProperty("fake.key")).thenReturn(false);
        assertFalse(mockPropertiesServiceAvailable.isAvailable(true));
    }
}
