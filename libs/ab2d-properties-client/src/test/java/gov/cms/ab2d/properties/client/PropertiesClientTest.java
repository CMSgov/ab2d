package gov.cms.ab2d.properties.client;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropertiesClientTest {
    @Disabled
    @Test
    void testGettingProperties() {
        final String testKey = "one";
        PropertiesClient client = new PropertiesClientImpl();
        Property property = client.getProperty("coverage.update.queueing");
        assertEquals("coverage.update.queueing", property.getKey());
        assertEquals("engaged", property.getValue());
        Property oneProp = client.setProperty(testKey, "two");
        assertNotNull(oneProp);
        assertEquals(testKey, oneProp.getKey());
        assertEquals("two", oneProp.getValue());
        Property onePropLoad = client.getProperty(testKey);
        assertEquals(testKey, onePropLoad.getKey());
        assertEquals("two", onePropLoad.getValue());
        Property onePropUpdate = client.setProperty(testKey, "three");
        assertEquals(testKey, onePropUpdate.getKey());
        assertEquals("three", onePropUpdate.getValue());
        Property onePropUpdateLoad = client.getProperty(testKey);
        assertEquals(testKey, onePropUpdateLoad.getKey());
        assertEquals("three", onePropUpdateLoad.getValue());
        client.deleteProperty(testKey);
        assertThrows(PropertyNotFoundException.class, () -> client.getProperty(testKey));
    }
}
