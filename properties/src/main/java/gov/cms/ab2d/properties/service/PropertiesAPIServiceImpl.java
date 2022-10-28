package gov.cms.ab2d.properties.service;

import gov.cms.ab2d.properties.client.PropertiesClient;
import gov.cms.ab2d.properties.client.Property;
import gov.cms.ab2d.properties.dto.PropertiesDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import gov.cms.ab2d.properties.client.PropertiesClientImpl;

import java.util.List;

@Slf4j
@Service
public class PropertiesAPIServiceImpl implements PropertiesAPIService {
    private PropertiesService propertiesService;
    private boolean usePropertyService;
    private PropertiesClient propertiesClient;

    PropertiesAPIServiceImpl(@Value("${feature.property.service.enabled:false}") boolean usePropertyService, PropertiesService service,
                             @Value("${property.service.url}") String propertyServiceUrl) {
        this.propertiesService = service;
        this.usePropertyService = usePropertyService;
        this.propertiesClient = new PropertiesClientImpl(propertyServiceUrl);
    }

    @Override
    public String getProperty(String property) {
        String value;
        if (usePropertyService) {
            try {
                Property prop = propertiesClient.getProperty(property);
                if (prop != null) {
                    value = prop.getValue();
                    log.info("Getting value for: " + property + " from properties service, value: " + value);
                } else {
                    log.error("Cannot access properties service, using default database value");
                    value = propertiesService.getPropertiesByKey(property).getValue();
                }
            } catch (Exception ex) {
                log.error("Cannot access properties service, using default database value", ex);
                value = propertiesService.getPropertiesByKey(property).getValue();
            }
        } else {
            value = propertiesService.getPropertiesByKey(property).getValue();
        }
        return value;
    }

    @Override
    public boolean updateProperty(String property, String value) {
        if (usePropertyService) {
            try {
                propertiesClient.setProperty(property, value);
                return true;
            } catch (Exception ex) {
                log.error("Cannot access properties service, using default database value", ex);
                PropertiesDTO propertiesDTO = new PropertiesDTO(property, value);
                return propertiesService.updateProperty(propertiesDTO);
            }
        }
        PropertiesDTO propertiesDTO = new PropertiesDTO(property, value);
        return propertiesService.updateProperty(propertiesDTO);
    }

    @Override
    public List<PropertiesDTO> getAllProperties() {
        if (usePropertyService) {
            List<Property> properties = propertiesClient.getAllProperties();
            if (properties != null && !properties.isEmpty()) {
                return properties.stream().map(p -> {
                    PropertiesDTO dto = new PropertiesDTO(p.getKey(), p.getValue());
                    return dto;
                }).toList();
            } else {
                log.error("Cannot access properties service, using default database value");
                return propertiesService.getAllPropertiesDTO();
            }
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
                log.error("Cannot access properties service, using default database value", ex);
                return propertiesService.insertProperty(key, value);
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
                log.error("Cannot access properties service, using default database value", ex);
                return propertiesService.deleteProperty(key);
            }
        }
        return propertiesService.deleteProperty(key);
    }
}
