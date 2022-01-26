package gov.cms.ab2d.common.service;

import com.google.common.reflect.TypeToken;
import gov.cms.ab2d.common.config.Mapping;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.repository.PropertiesRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static gov.cms.ab2d.common.util.Constants.*;
import static java.lang.Boolean.FALSE;

@AllArgsConstructor
@Service
@Transactional
@Slf4j
public class PropertiesServiceImpl implements PropertiesService {

    private final Mapping mapping;

    private final PropertiesRepository propertiesRepository;

    private final Type propertiesListType = new TypeToken<List<PropertiesDTO>>() { } .getType();

    @Override
    public boolean isInMaintenanceMode() {
        return Boolean.valueOf(getPropertiesByKey(MAINTENANCE_MODE).getValue());
    }

    @Override
    public List<Properties> getAllProperties() {
        return propertiesRepository.findAll();
    }

    @Override
    public List<PropertiesDTO> getAllPropertiesDTO() {
        List<Properties> properties = getAllProperties();
        return mapping.getModelMapper().map(properties, propertiesListType);
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
            validateString(key, propertiesDTO);
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

    // Seems wrong to validate the specific values of the enum in a common class, so just do a null check
    private void validateString(String key, PropertiesDTO property) {
        if (property.getValue() == null) {
            logErrorAndThrowException(key, property.getValue());
        }
    }

    void validateInt(String var, PropertiesDTO property, int min, int max) {
        Integer val = Integer.valueOf(property.getValue());
        if (property == null || val < min || val > max) {
            logErrorAndThrowException(var, val);
        }
    }

    void validateBoolean(String var, PropertiesDTO property) {
        String val = property.getValue();
        if (property == null || !(val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false"))) {
            logErrorAndThrowException(var, val);
        }
    }

    void checkNameOfPropertyKey(PropertiesDTO properties) {
        if (!ALLOWED_PROPERTY_NAMES.contains(properties.getKey())) {
            log.error("Properties must contain a valid key name, received {}", properties.getKey());
            throw new InvalidPropertiesException("Properties must contain a valid key name, received " + properties.getKey());
        }
    }

    private void addUpdatedPropertiesToList(List<PropertiesDTO> propertiesDTOsReturn, PropertiesDTO propertiesDTO) {
        Properties properties = getPropertiesByKey(propertiesDTO.getKey());
        Properties mappedProperties = mapping.getModelMapper().map(propertiesDTO, Properties.class);
        mappedProperties.setId(properties.getId());
        Properties updatedProperties = propertiesRepository.save(mappedProperties);

        log.info("Updated property {} with value {}", updatedProperties.getKey(), updatedProperties.getValue());

        propertiesDTOsReturn.add(mapping.getModelMapper().map(updatedProperties, PropertiesDTO.class));
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
                .orElse(FALSE)
                .booleanValue();
    }
}
