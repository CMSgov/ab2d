package gov.cms.ab2d.common.properties;

import gov.cms.ab2d.common.repository.PropertiesRepository;
import gov.cms.ab2d.common.model.Property;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class PropertiesServiceImpl implements PropertiesService {

    private final PropertiesRepository propertiesRepository;

    public PropertiesServiceImpl(PropertiesRepository propertiesRepository) {
        this.propertiesRepository = propertiesRepository;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        val property = getProperty(key);
        if (property.isPresent()) {
            return property.get().getValue();
        } else {
            log.warn("Property '{}' not found; using default", key);
            return defaultValue;
        }
    }

    @Override
    public boolean updateProperty(String key, String value) {
        val property = getProperty(key);
        if (property.isEmpty()) {
            log.error("Unable to update '{}' - property not found", key);
            return false;
        }
        try {
            val entity = property.get();
            entity.setValue(value);
            propertiesRepository.saveAndFlush(entity);
            return true;
        } catch (Exception e) {
            log.error("Error updating property '{}'", key, e);
            return false;
        }
    }

    @Override
    public boolean isToggleOn(final String toggleName, boolean defaultValue) {
        if (StringUtils.isEmpty(toggleName)) {
            return false;
        }
        String val = getProperty(toggleName, "" + defaultValue);
        return Boolean.parseBoolean(val.trim());
    }

    protected Optional<Property> getProperty(String property) {
        try {
            return propertiesRepository.findByKey(property);
        } catch (Exception e) {
            log.error("Error retrieving property '{}'", property, e);
            return Optional.empty();
        }
    }
}
