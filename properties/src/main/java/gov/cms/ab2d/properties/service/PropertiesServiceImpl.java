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
import java.util.ArrayList;
import java.util.List;

import static gov.cms.ab2d.properties.util.Constants.*;
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
    public boolean isInMaintenanceMode() {
        return Boolean.parseBoolean(getPropertiesByKey(MAINTENANCE_MODE).getValue());
    }

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
    public List<PropertiesDTO> updateProperties(List<PropertiesDTO> propertiesDTOs) {
        List<PropertiesDTO> propertiesDTOsReturn = new ArrayList<>();
        for (PropertiesDTO propertiesDTO : propertiesDTOs) {
            checkNameOfPropertyKey(propertiesDTO);
            String key = propertiesDTO.getKey();
            validateProperty(propertiesDTOsReturn, propertiesDTO, key);
        }
        return propertiesDTOsReturn;
    }

    private void validateProperty(List<PropertiesDTO> propertiesDTOsReturn, PropertiesDTO propertiesDTO, String key) {
        // If this becomes more extensive, consider having a table that contains a mapping of keys to validation expressions
        switch (key) {
        case PCP_CORE_POOL_SIZE:
            validateInt(key, propertiesDTO, 1, 100);
            addUpdatedPropertiesToList(propertiesDTOsReturn, propertiesDTO);
            break;

        case PCP_MAX_POOL_SIZE:
            validateInt(key, propertiesDTO, 1, 500);
            addUpdatedPropertiesToList(propertiesDTOsReturn, propertiesDTO);
            break;

        case PCP_SCALE_TO_MAX_TIME:
            validateInt(key, propertiesDTO, 1, 3600);
            addUpdatedPropertiesToList(propertiesDTOsReturn, propertiesDTO);
            break;

        case MAINTENANCE_MODE:
        case ZIP_SUPPORT_ON:
        case COVERAGE_SEARCH_OVERRIDE:
            validateBoolean(key, propertiesDTO);
            addUpdatedPropertiesToList(propertiesDTOsReturn, propertiesDTO);
            break;

        case WORKER_ENGAGEMENT:
        case HPMS_INGESTION_ENGAGEMENT:
        case COVERAGE_SEARCH_DISCOVERY:
        case COVERAGE_SEARCH_QUEUEING:
            addUpdatedPropertiesToList(propertiesDTOsReturn, propertiesDTO);
            break;
        // The maximums for these values are arbitrary and may need to be changed
        case COVERAGE_SEARCH_UPDATE_MONTHS:
            validateInt(key, propertiesDTO, 0, 12);
            addUpdatedPropertiesToList(propertiesDTOsReturn, propertiesDTO);
            break;
        case COVERAGE_SEARCH_STUCK_HOURS:
            validateInt(key, propertiesDTO, 12, 168);
            addUpdatedPropertiesToList(propertiesDTOsReturn, propertiesDTO);
            break;
        default:
            break;
        }
    }

    void validateInt(String key, PropertiesDTO property, int min, int max) {
        int val = Integer.parseInt(property.getValue());
        if (val < min || val > max) {
            logErrorAndThrowException(key, val);
        }
    }

    void validateBoolean(String key, PropertiesDTO property) {
        String val = property.getValue();
        if (!(val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false"))) {
            logErrorAndThrowException(key, val);
        }
    }

    void checkNameOfPropertyKey(PropertiesDTO properties) {
        List<Properties> allProps = getAllProperties();
        boolean validKey = allProps.stream().filter(p -> p.getKey().equals(properties.getKey())).findAny().isPresent();
        if (!validKey) {
            log.error("Properties must contain a valid key name, received {}", properties.getKey());
            throw new InvalidPropertiesException("Properties must contain a valid key name, received " + properties.getKey());
        }
    }

    private void addUpdatedPropertiesToList(List<PropertiesDTO> propertiesDTOsReturn, PropertiesDTO propertiesDTO) {
        Properties properties = getPropertiesByKey(propertiesDTO.getKey());
        Properties mappedProperties = mapper.map(propertiesDTO, Properties.class);
        mappedProperties.setId(properties.getId());
        Properties updatedProperties = propertiesRepository.save(mappedProperties);

        log.info("Updated property {} with value {}", updatedProperties.getKey(), updatedProperties.getValue());

        propertiesDTOsReturn.add(mapper.map(updatedProperties, PropertiesDTO.class));
    }

    private void logErrorAndThrowException(String propertyKey, Object propertyValue) {
        log.error("Incorrect value for {} of {}", propertyKey, propertyValue);
        throw new InvalidPropertiesException("Incorrect value for " + propertyKey + " of " + propertyValue);
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
