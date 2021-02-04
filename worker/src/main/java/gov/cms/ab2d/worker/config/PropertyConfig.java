package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.worker.properties.PropertiesInit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

// Separate this config out so that it runs before variables in the main config with the @Value annotation are evaluated
@Configuration
public class PropertyConfig {

    @Bean
    public PropertiesInit propertiesInit(ConfigurableEnvironment configurableEnvironment,
                                         PropertiesService propertiesService) {
        PropertiesInit propertiesInit = new PropertiesInit(propertiesService, configurableEnvironment);
        propertiesInit.updatePropertiesFromDatabase();
        return propertiesInit;
    }
}
