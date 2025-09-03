package gov.cms.ab2d.common.properties;

import gov.cms.ab2d.properties.client.Property;

import java.util.List;

public interface PropertiesClient {
    Property getProperty(String key);
    Property setProperty(String key, String value);
}
