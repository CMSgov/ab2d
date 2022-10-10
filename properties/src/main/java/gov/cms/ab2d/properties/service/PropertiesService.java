package gov.cms.ab2d.properties.service;

import gov.cms.ab2d.properties.dto.PropertiesDTO;
import gov.cms.ab2d.properties.model.Properties;

import java.util.List;

public interface PropertiesService {

    List<Properties> getAllProperties();

    List<PropertiesDTO> getAllPropertiesDTO();

    Properties getPropertiesByKey(String key);

    boolean updateProperty(PropertiesDTO propertiesDTO);

    boolean isToggleOn(String toggleName);

    boolean insertProperty(String key, String value);

    boolean deleteProperty(String key);
}
