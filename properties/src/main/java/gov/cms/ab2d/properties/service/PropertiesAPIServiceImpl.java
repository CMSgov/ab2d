package gov.cms.ab2d.properties.service;

import gov.cms.ab2d.properties.dto.PropertiesDTO;
import gov.cms.ab2d.properties.model.Properties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class PropertiesAPIServiceImpl implements PropertiesAPIService {
    private PropertiesService propertiesService;

    @Override
    public String getProperty(String property) {
        Properties properties = propertiesService.getPropertiesByKey(property);
        return properties.getValue();
    }

    @Override
    public boolean updateProperty(String property, String value) {
        PropertiesDTO propertiesDTO = new PropertiesDTO();
        propertiesDTO.setKey(property);
        propertiesDTO.setValue(value);

        List<PropertiesDTO> savedProps = propertiesService.updateProperties(Collections.singletonList(propertiesDTO));
        try {
            return (savedProps.size() == 1 && savedProps.get(0).getValue().equals(value));
        } catch (Exception ex) {
            log.error("Unable to save property", ex);
            return false;
        }
    }

    @Override
    public boolean isInMaintenanceMode() {
        return propertiesService.isInMaintenanceMode();
    }

    @Override
    public List<PropertiesDTO> getAllProperties() {
        return propertiesService.getAllPropertiesDTO();
    }

    @Override
    public List<PropertiesDTO> updateProperties(List<PropertiesDTO> properties) {
        return propertiesService.updateProperties(properties);
    }

    @Override
    public boolean isToggleOn(final String toggleName) {
        String val = getProperty(toggleName);
        if (val == null) {
            return false;
        }
        return Boolean.valueOf(val.trim());
    }

    @Override
    public boolean createProperty(String key, String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return false;
        }
        return propertiesService.insertProperty(key, value);
    }

    @Override
    public boolean deleteProperty(String key) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        return propertiesService.deleteProperty(key);
    }
}
