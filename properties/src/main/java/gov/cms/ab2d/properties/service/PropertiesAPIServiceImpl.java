package gov.cms.ab2d.properties.service;

import gov.cms.ab2d.properties.dto.PropertiesDTO;
import gov.cms.ab2d.properties.model.Properties;
import gov.cms.ab2d.properties.client.PropertiesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PropertiesAPIServiceImpl implements PropertiesAPIService {
    private PropertiesService propertiesService;
    private boolean usePropertyService;
    private String propertyServiceUrl;
    private PropertiesClient propertiesClient;

    PropertiesAPIServiceImpl(@Value("${feature.property.service.enabled}") boolean usePropertyService, PropertiesService service,
                             @Value("${property.service.url}") String propertyServiceUrl) {
        this.propertiesService = service;
        this.usePropertyService = usePropertyService;
        this.propertyServiceUrl = propertyServiceUrl;
        this.propertiesClient = new PropertiesClientImpl(propertyServiceUrl);
    }

    @Override
    public String getProperty(String property) {
        if (usePropertyService) {
            try {
                return propertiesClient.getProperty(property);
            } catch (Exception ex) {
                return null;
            }
        }
        Properties properties = propertiesService.getPropertiesByKey(property);
        return properties.getValue();
    }

    @Override
    public boolean updateProperty(String property, String value) {
        if (usePropertyService) {
            try {
                propertiesClient.setProperty(property, value);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
        PropertiesDTO propertiesDTO = new PropertiesDTO(property, value);
        return propertiesService.updateProperty(propertiesDTO);
    }

    @Override
    public List<PropertiesDTO> getAllProperties() {
        if (usePropertyService) {
            return propertiesClient.getAllProperties();
        }
        return propertiesService.getAllPropertiesDTO();
    }

    @Override
    public boolean isToggleOn(final String toggleName) {
        if (StringUtils.isEmpty(toggleName)) {
            return false;
        }
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
        if (usePropertyService) {
            try {
                propertiesClient.setProperty(key, value);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
        return propertiesService.insertProperty(key, value);
    }

    @Override
    public boolean deleteProperty(String key) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        if (usePropertyService) {
            try {
                propertiesClient.deleteProperty(key);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
        return propertiesService.deleteProperty(key);
    }
}
