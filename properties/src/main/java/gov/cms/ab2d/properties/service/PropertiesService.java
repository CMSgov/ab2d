package gov.cms.ab2d.properties.service;

import gov.cms.ab2d.properties.dto.PropertiesDTO;
import gov.cms.ab2d.properties.model.Properties;

import java.util.List;

public interface PropertiesService {

    boolean isInMaintenanceMode();

    List<Properties> getAllProperties();

    List<PropertiesDTO> getAllPropertiesDTO();

    Properties getPropertiesByKey(String key);

    List<PropertiesDTO> updateProperties(List<PropertiesDTO> propertiesDTOs);

    boolean isToggleOn(String toggleName);

    boolean insertProperty(String key, String value);

    boolean deleteProperty(String key);
}
