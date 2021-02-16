package gov.cms.ab2d.worker.properties;

import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.service.PropertiesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class PropertiesInit {

    private final ConfigurableEnvironment configurableEnvironment;

    private final PropertiesService propertiesService;

    public PropertiesInit(PropertiesService propertiesService, ConfigurableEnvironment configurableEnvironment) {
        this.propertiesService = propertiesService;
        this.configurableEnvironment = configurableEnvironment;
    }

    // Load all of the properties from the database and insert/overwrite
    public Map<String, Object> updatePropertiesFromDatabase() {
        log.debug("Updating properties");
        final Map<String, Object> properties = propertiesService.getAllProperties().stream()
                .collect(Collectors.toMap(Properties::getKey, Properties::getValue));
        configurableEnvironment.getPropertySources().addLast(new MapPropertySource("application", properties));

        return properties;
    }
}
