package gov.cms.ab2d.properties.service;

import com.google.common.reflect.TypeToken;
import gov.cms.ab2d.properties.repository.PropertiesRepository;
import gov.cms.ab2d.properties.dto.PropertiesDTO;
import gov.cms.ab2d.properties.model.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.FALSE;

@Service
@Transactional
@Slf4j
public class PropertiesServiceImpl implements PropertiesService {

    private final ModelMapper mapper = new ModelMapper();

    private final PropertiesRepository propertiesRepository;

    public PropertiesServiceImpl(PropertiesRepository propertiesRepository) {
        this.propertiesRepository = propertiesRepository;
    }

    @SuppressWarnings("UnstableApiUsage")
    private final Type propertiesListType = new TypeToken<List<PropertiesDTO>>() { } .getType();

    @Override
    public List<Properties> getAllProperties() {
        return propertiesRepository.findAll();
    }

    @Override
    public List<PropertiesDTO> getAllPropertiesDTO() {
        List<Properties> properties = getAllProperties();
        return mapper.map(properties, propertiesListType);
    }

    @Override
    public Properties getPropertiesByKey(String key) {
        return propertiesRepository.findByKey(key).orElseThrow(() -> {
            log.error("No entry was found for key {}", key);
            return new ResourceNotFoundException("No entry was found for key " + key);
        });
    }

    @Override
    public boolean updateProperty(PropertiesDTO propertyDTO) {
        Properties prop = checkNameOfPropertyKey(propertyDTO);
        prop.setValue(propertyDTO.getValue());
        Properties updatedProperties = propertiesRepository.save(prop);

        log.info("Updated property {} with value {}", updatedProperties.getKey(), updatedProperties.getValue());
        return updatedProperties.getId() != 0;
    }

    Properties checkNameOfPropertyKey(PropertiesDTO properties) {
        List<Properties> allProps = getAllProperties();
        Optional<Properties> found = allProps.stream().filter(p -> p.getKey().equals(properties.getKey())).findFirst();
        if (found.isEmpty()) {
            log.error("Properties must contain a valid key name, received {}", properties.getKey());
            throw new InvalidPropertiesException("Properties must contain a valid key name, received " + properties.getKey());
        }
        return found.get();
    }

    public boolean isToggleOn(final String toggleName) {
        return propertiesRepository.findByKey(toggleName)
                .map(Properties::getValue)
                .map(StringUtils::trim)
                .map(Boolean::valueOf)
                .orElse(FALSE);
    }

    @Override
    public boolean insertProperty(String key, String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            log.error("Error inserting property - The key: {}, value: {}", key, value);
            return false;
        }
        List<Properties> allProps = getAllProperties();
        boolean validKey = allProps.stream().anyMatch(p -> p.getKey().equals(key));

        if (validKey) {
            log.error("{} already exists", key);
            return false;
        }
        Properties properties = new Properties();
        properties.setKey(key);
        properties.setValue(value);
        Properties prop = propertiesRepository.save(properties);
        return prop.getId() != 0;
    }

    public boolean deleteProperty(String key) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        Properties prop = getPropertiesByKey(key);
        if (prop.getId() == 0) {
            return false;
        }
        propertiesRepository.deleteById(prop.getId());
        return true;
    }
}
