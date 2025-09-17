package gov.cms.ab2d.contracts.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import static gov.cms.ab2d.contracts.config.SwaggerConstants.MAIN;

/**
 * Configuration for the swagger ui dictating how annnotations are processed and default security/responses.
 *
 * {@link OpenAPI} - the base configuration for all versions of the API
 * {@link GroupedOpenApi} - one of these for each version of FHIR we support (V1 - STU3, V2 - R4)
 * {@link OpenApiCustomiser} - customize a {@link GroupedOpenApi} with default behavior
 */
@Slf4j
@Configuration
public class OpenAPIConfig {

    /**
     * Configuration for the whole swagger page
     */
    @Bean
    public OpenAPI ab2dAPI() {
        return new OpenAPI()
                .components(new Components()
                ).info(new Info().title("Contracts Service API").description(MAIN));
    }

    /**
     * Limit to STU3 aspects of the API
     */
    @Bean
    public GroupedOpenApi apiV1() {
        return GroupedOpenApi.builder()
                .group("V1")
                .packagesToScan("gov.cms.ab2d.contracts.controller")
                .pathsToMatch("/**")
                .build();
    }

}
