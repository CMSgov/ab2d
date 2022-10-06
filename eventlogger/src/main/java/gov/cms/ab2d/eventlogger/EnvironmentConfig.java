package gov.cms.ab2d.eventlogger;

import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvironmentConfig {

    /**
     * Parse execution environment passed in during
     * @param executionEnv string value of execution environment that must match the name of one {@link Ab2dEnvironment}
     * @return current ab2d environment
     */
    @Bean
    public Ab2dEnvironment getEnvironment(@Value("${execution.env}") String executionEnv) {
        return Ab2dEnvironment.fromName(executionEnv);
    }
}
