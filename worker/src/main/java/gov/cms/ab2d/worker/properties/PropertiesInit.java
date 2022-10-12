package gov.cms.ab2d.worker.properties;

import gov.cms.ab2d.properties.dto.PropertiesDTO;
import gov.cms.ab2d.properties.service.PropertiesAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class PropertiesInit {

    private final ConfigurableEnvironment configurableEnvironment;

    private final PropertiesAPIService propertiesApiService;

    public PropertiesInit(PropertiesAPIService propertiesApiService, ConfigurableEnvironment configurableEnvironment) {
        this.propertiesApiService = propertiesApiService;
        this.configurableEnvironment = configurableEnvironment;
    }

    // Load all of the properties from the database and insert/overwrite
    public Map<String, Object> updatePropertiesFromDatabase() {
        log.debug("Updating properties");
        final Map<String, Object> properties = propertiesApiService.getAllProperties().stream()
                .collect(Collectors.toMap(PropertiesDTO::getKey, PropertiesDTO::getValue));
        configurableEnvironment.getPropertySources().addLast(new MapPropertySource("application", properties));

        return properties;
    }
}
