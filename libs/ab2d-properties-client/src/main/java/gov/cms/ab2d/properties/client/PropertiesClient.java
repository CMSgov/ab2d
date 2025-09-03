package gov.cms.ab2d.properties.client;

import java.util.List;

public interface PropertiesClient {
    List<Property> getAllProperties();
    Property getProperty(String key);
    Property setProperty(String key, String value);
    void deleteProperty(String key);
}
