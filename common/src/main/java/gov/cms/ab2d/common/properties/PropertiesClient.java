package gov.cms.ab2d.common.properties;

import gov.cms.ab2d.common.model.Property;

public interface PropertiesClient {
    Property getProperty(String key);
    Property setProperty(String key, String value);
}
