package gov.cms.ab2d.properties.service;

import gov.cms.ab2d.properties.dto.PropertiesDTO;

import java.util.List;

public interface PropertiesAPIService {
    String getProperty(String property);
    boolean updateProperty(String property, String value);
    List<PropertiesDTO> getAllProperties();
    boolean isToggleOn(final String toggleName);
    boolean createProperty(String key, String value);
    boolean deleteProperty(String key);
}
