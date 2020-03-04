package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.Properties;

import java.util.List;

public interface PropertiesService {

    boolean isInMaintenanceMode();

    List<Properties> getAllProperties();

    List<PropertiesDTO> getAllPropertiesDTO();

    Properties getPropertiesByKey(String key);

    List<PropertiesDTO> updateProperties(List<PropertiesDTO> propertiesDTOs);

    boolean isToggleOn(String toggleName);
}
