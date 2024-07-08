package gov.cms.ab2d.common.properties;

import gov.cms.ab2d.properties.client.PropertiesClient;
import gov.cms.ab2d.properties.client.Property;
import gov.cms.ab2d.properties.client.PropertyNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PropertiesServiceTest {
    @Mock
    PropertiesClient propertiesClient;

    PropertiesService propertiesService = new PropertiesServiceImpl("http://localhost");

    Property val1 = new Property("item1", "val1");
    Property val2 = new Property("item2", "val2");
    Property val3 = new Property("item3", "val3");
    Property val4 = new Property("item4", "val4");

    String defValue = "DEFAULT";
    @BeforeEach
    void setMock() {
        ReflectionTestUtils.setField(propertiesService, "propertiesClient", propertiesClient);
    }

    @AfterEach
    void resetMocks() {
        reset(propertiesClient);
    }

    @Test
    void basicTest() {

        when(propertiesClient.getProperty(val1.getKey())).thenReturn(val1);
        when(propertiesClient.getProperty(val4.getKey())).thenThrow(PropertyNotFoundException.class);
        assertEquals(val1.getValue(), propertiesService.getProperty(val1.getKey(), defValue));
        assertEquals(defValue, propertiesService.getProperty(val4.getKey(), defValue));

        when(propertiesClient.getProperty(val2.getKey())).thenReturn(val2);
        when(propertiesClient.setProperty(val2.getKey(), val2.getValue())).thenReturn(val2);
        assertTrue(propertiesService.updateProperty(val2.getKey(), val2.getValue()));

        when(propertiesClient.getProperty(val3.getKey())).thenReturn(val3);
        when(propertiesClient.setProperty(val3.getKey(), val3.getValue())).thenReturn(null);
        assertFalse(propertiesService.updateProperty(val3.getKey(), val3.getValue()));
        when(propertiesClient.setProperty(val4.getKey(), val4.getValue())).thenThrow(PropertyNotFoundException.class);
        assertFalse(propertiesService.updateProperty(val4.getKey(), val4.getValue()));

        when(propertiesClient.getAllProperties()).thenReturn(List.of(val2, val2, val3, val4)).thenReturn(null);
        List<PropertiesDTO> allprops = propertiesService.getAllProperties();
        assertEquals(4, allprops.size());
        assertEquals(0, allprops.stream().filter(c -> c.getKey() == null || c.getValue() == null).count());
        assertEquals(0, propertiesService.getAllProperties().size());

        assertFalse(propertiesService.isToggleOn(null, false));
        assertFalse(propertiesService.isToggleOn(val4.getKey(), false));
    }

    @Test
    void createProperties() {
        when(propertiesClient.getProperty(val1.getKey())).thenReturn(null).thenReturn(val1);
        when(propertiesClient.setProperty(val1.getKey(), val1.getValue())).thenReturn(val1);
        assertTrue(propertiesService.createProperty(val1.getKey(), val1.getValue()));
        assertTrue(propertiesService.createProperty(val1.getKey(), val1.getValue()));
        assertFalse(propertiesService.createProperty(val1.getKey(), null));
        assertFalse(propertiesService.createProperty(null, val1.getValue()));
    }

    @Test
    void createPropertiesError() {
        assertFalse(propertiesService.createProperty("", val1.getValue()));

        when(propertiesClient.setProperty(val1.getKey(), val1.getValue())).thenReturn(null);
        assertFalse(propertiesService.createProperty(val1.getKey(), val1.getValue()));
        when(propertiesClient.setProperty(val1.getKey(), val1.getValue())).thenThrow(PropertyNotFoundException.class);
        assertFalse(propertiesService.createProperty(val1.getKey(), val1.getValue()));
    }

    @Test
    void testDeleteProperty() {
        assertFalse(propertiesService.deleteProperty(null));
        assertTrue(propertiesService.deleteProperty(val1.getKey()));
        doThrow(PropertyNotFoundException.class).when(propertiesClient).deleteProperty(val1.getKey());
        assertFalse(propertiesService.deleteProperty(val1.getKey()));
    }
}
