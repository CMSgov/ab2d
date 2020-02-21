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

    // Load all of the properties from the database
    public PropertiesInit(PropertiesService propertiesService, ConfigurableEnvironment configurableEnvironment) {
        log.info("**************Adding properties");
        final Map<String, Object> properties = propertiesService.getAllProperties().stream()
                .collect(Collectors.toMap(Properties::getKey, Properties::getValue));
        configurableEnvironment.getPropertySources().addFirst(new MapPropertySource("application", properties));
    }
}
