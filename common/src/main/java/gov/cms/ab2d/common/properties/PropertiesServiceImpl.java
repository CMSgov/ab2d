package gov.cms.ab2d.common.properties;

import gov.cms.ab2d.properties.client.PropertiesClient;
import gov.cms.ab2d.properties.client.PropertiesClientImpl;
import gov.cms.ab2d.properties.client.Property;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PropertiesServiceImpl implements PropertiesService {
    private PropertiesClient propertiesClient;
    private static final String ERROR_MESSAGE = "Cannot access properties service, using default database value";

    PropertiesServiceImpl(@Value("${property.service.url}") String propertyServiceUrl) {
        this.propertiesClient = new PropertiesClientImpl(propertyServiceUrl);
    }

    @Override
    public String getProperty(String property, String defaultValue) {
        String value;
        try {
            return propertiesClient.getProperty(property).getValue();
        } catch (Exception ex) {
            log.error(ERROR_MESSAGE, ex);
            return defaultValue;
        }
    }

    @Override
    public boolean updateProperty(String property, String value) {
        try {
            Property prop = propertiesClient.setProperty(property, value);
            return prop != null;
        } catch (Exception ex) {
            log.error(ERROR_MESSAGE, ex);
            return false;
        }
    }

    @Override
    public List<PropertiesDTO> getAllProperties() {
        List<Property> properties = propertiesClient.getAllProperties();
        if (properties != null && !properties.isEmpty()) {
            return properties.stream().map(p -> {
                return new PropertiesDTO(p.getKey(), p.getValue());
            }).toList();
        } else {
            log.error(ERROR_MESSAGE);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean isToggleOn(final String toggleName, boolean defaultValue) {
        if (StringUtils.isEmpty(toggleName)) {
            return false;
        }
        String val = getProperty(toggleName, "" + defaultValue);
        return Boolean.valueOf(val.trim());
    }

    @Override
    public boolean createProperty(String key, String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return false;
        }
        try {
            Property prop = propertiesClient.setProperty(key, value);
            if (prop != null) {
                return true;
            } else {
                log.error(ERROR_MESSAGE);
                return false;
            }
        } catch (Exception ex) {
            log.error(ERROR_MESSAGE, ex);
            return false;
        }
    }

    @Override
    public boolean deleteProperty(String key) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        try {
            propertiesClient.deleteProperty(key);
            return true;
        } catch (Exception ex) {
            log.error(ERROR_MESSAGE, ex);
            return false;
        }
    }
}
