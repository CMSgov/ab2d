package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.worker.properties.PropertiesInit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;

// Separate this config out so that it runs before variables in the main config with the @Value annotation are evaluated
//@Configuration
public class PropertyConfig {

    @Autowired
    private ConfigurableEnvironment configurableEnvironment;

    @Autowired
    private PropertiesService propertiesService;

    //@Bean
    public PropertiesInit propertiesInit() {
        PropertiesInit propertiesInit = new PropertiesInit(propertiesService, configurableEnvironment);
        return propertiesInit;
    }
}
