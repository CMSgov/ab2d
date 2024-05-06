package gov.cms.ab2d.api.config;

import com.fasterxml.jackson.annotation.*;
import gov.cms.ab2d.api.controller.JobCompletedResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

import static gov.cms.ab2d.api.util.Constants.GENERIC_FHIR_ERR_MSG;
import static gov.cms.ab2d.api.util.SwaggerConstants.MAIN;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V1;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V2;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

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
        final String BEARER_AUTH = "bearerAuth";
        return new OpenAPI()
                // Dictate that all endpoints within Swagger must make authenticated calls using
                // a bearer token.
                // This corresponds to the "Authorize" button in Swagger
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .name(BEARER_AUTH)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                // The main lorem ipsum for the page
                ).info(new Info().title("AB2D API").description(MAIN));
    }

    /**
     * Limit to STU3 aspects of the API
     */
    @Bean
    public GroupedOpenApi apiV1() {
        return GroupedOpenApi.builder()
                .group("V1 - FHIR STU3")
                .packagesToScan("gov.cms.ab2d.api.controller")
                // Only match /v1/fhir calls
                .pathsToMatch(API_PREFIX_V1 + FHIR_PREFIX + "/**")
                // Customize the page with default error responses to authentication and internal errors
                .addOpenApiCustomiser(defaultResponseMessages())
                .build();
    }

    /**
     * Limit to R4 aspects of the API
     */
    @Bean
    public GroupedOpenApi apiV2() {
        return GroupedOpenApi.builder()
                .group("V2 - FHIR R4")
                .packagesToScan("gov.cms.ab2d.api.controller")
                // Only match /v2/fhir calls
                .pathsToMatch(API_PREFIX_V2 + FHIR_PREFIX + "/**")
                // Customize the page with default error responses to authentication and internal errors
                .addOpenApiCustomiser(defaultResponseMessages())
                .build();
    }

    public OpenApiCustomiser defaultResponseMessages() {
        return api -> {

            // Add JSON templates for expected response bodies
            // These can be referenced by the standard location ex. #/components/schemas/OperationOutcome
            api.getComponents().getSchemas().putAll(ModelConverters.getInstance().read(OperationOutcome.class));
            api.getComponents().getSchemas().putAll(ModelConverters.getInstance().read(Issue.class));
            api.getComponents().getSchemas().putAll(ModelConverters.getInstance().read(Details.class));

            api.getComponents().getSchemas().putAll(ModelConverters.getInstance().read(JobCompletedResponse.class));
            api.getComponents().getSchemas().putAll(ModelConverters.getInstance().read(JobCompletedResponse.Output.class));
            api.getComponents().getSchemas().putAll(ModelConverters.getInstance().read(JobCompletedResponse.FileMetadata.class));

            // For each API endpoint on the page add a set of default responses to what is displayed
            api.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                ApiResponses responses = operation.getResponses();

                // Response schema that you can display template
                Schema schema = new Schema();
                schema.setName("OperationOutcome");
                schema.set$ref("#/components/schemas/OperationOutcome");

                MediaType mediaType = new MediaType();
                mediaType.schema(schema);

                // Default responses for standard error conditions
                ApiResponse internalError = new ApiResponse()
                        .description("An internal error occurred. " + GENERIC_FHIR_ERR_MSG)
                        .content(new Content().addMediaType(APPLICATION_JSON_VALUE, mediaType));
                responses.addApiResponse("500", internalError);

                ApiResponse requestError = new ApiResponse()
                        .description("There was a problem with the request. " + GENERIC_FHIR_ERR_MSG)
                        .content(new Content().addMediaType(APPLICATION_JSON_VALUE, mediaType));
                responses.addApiResponse("400", requestError);

                ApiResponse tokenError = new ApiResponse()
                        .description("Unauthorized. Missing authentication token.");
                responses.addApiResponse("401", tokenError);

                ApiResponse authError = new ApiResponse()
                        .description("Forbidden. Access not permitted.");
                responses.addApiResponse("403", authError);
            }));
        };
    }

    // FHIR's OperationOutcome won't play nice with Swagger. Having to redefine it here
    // to get the Swagger API spec looking right.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({
            "text"
    })
    static class Details {

        @JsonProperty("text")
        private String text;
        @JsonIgnore
        private Map<String, Object> additionalProperties = new HashMap<String, Object>();

        @JsonProperty("text")
        public String getText() {
            return text;
        }

        @JsonProperty("text")
        public void setText(String text) {
            this.text = text;
        }

        @JsonAnyGetter
        public Map<String, Object> getAdditionalProperties() {
            return this.additionalProperties;
        }

        @JsonAnySetter
        public void setAdditionalProperty(String name, Object value) {
            this.additionalProperties.put(name, value);
        }

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({
            "severity",
            "code",
            "details"
    })
    static class Issue {
        @JsonProperty("severity")
        private String severity;
        @JsonProperty("code")
        private String code;
        @JsonProperty("details")
        private Details details;
        @JsonIgnore
        private Map<String, Object> additionalProperties = new HashMap<String, Object>();

        @JsonProperty("severity")
        public String getSeverity() {
            return severity;
        }

        @JsonProperty("severity")
        public void setSeverity(String severity) {
            this.severity = severity;
        }

        @JsonProperty("code")
        public String getCode() {
            return code;
        }

        @JsonProperty("code")
        public void setCode(String code) {
            this.code = code;
        }

        @JsonProperty("details")
        public Details getDetails() {
            return details;
        }

        @JsonProperty("details")
        public void setDetails(Details details) {
            this.details = details;
        }

        @JsonAnyGetter
        public Map<String, Object> getAdditionalProperties() {
            return this.additionalProperties;
        }

        @JsonAnySetter
        public void setAdditionalProperty(String name, Object value) {
            this.additionalProperties.put(name, value);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({
            "resourceType",
            "id",
            "issue"
    })
    public class OperationOutcome {
        @JsonProperty("resourceType")
        private String resourceType;

        @JsonProperty("id")
        private String id;

        @JsonProperty("issue")
        private List<Issue> issue = null;

        @JsonIgnore
        private Map<String, Object> additionalProperties = new HashMap<String, Object>();

        @JsonProperty("resourceType")
        public String getResourceType() {
            return resourceType;
        }

        @JsonProperty("resourceType")
        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        @JsonProperty("id")
        public String getId() {
            return id;
        }

        @JsonProperty("id")
        public void setId(String id) {
            this.id = id;
        }

        @JsonProperty("issue")
        public List<Issue> getIssue() {
            return issue;
        }

        @JsonProperty("issue")
        public void setIssue(List<Issue> issue) {
            this.issue = issue;
        }

        @JsonAnyGetter
        public Map<String, Object> getAdditionalProperties() {
            return this.additionalProperties;
        }

        @JsonAnySetter
        public void setAdditionalProperty(String name, Object value) {
            this.additionalProperties.put(name, value);
        }

    }
}
