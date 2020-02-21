package gov.cms.ab2d.worker.properties;

import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.service.PropertiesService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;
import java.util.stream.Collectors;

public class PropertiesInit {

    // Load all of the properties from the database
    public PropertiesInit(PropertiesService propertiesService, ConfigurableEnvironment configurableEnvironment) {
        final Map<String, Object> properties = propertiesService.getAllProperties().stream()
                .collect(Collectors.toMap(Properties::getKey, Properties::getValue));
        configurableEnvironment.getPropertySources().addLast(new MapPropertySource("application", properties));
    }
}
