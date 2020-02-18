package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.worker.properties.PropertiesInit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

@Configuration
public class PropertyConfig {

    @Autowired
    private ConfigurableEnvironment configurableEnvironment;

    @Autowired
    private PropertiesService propertiesService;

    @Bean
    public PropertiesInit propertiesInit() {
        PropertiesInit propertiesInit = new PropertiesInit(propertiesService, configurableEnvironment);
        return propertiesInit;
    }
}
