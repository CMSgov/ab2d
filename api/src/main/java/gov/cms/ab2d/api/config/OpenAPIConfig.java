package gov.cms.ab2d.api.config;

import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.jackson.annotation.*;
import gov.cms.ab2d.api.util.Constants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.AllArgsConstructor;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

import static gov.cms.ab2d.api.util.SwaggerConstants.MAIN;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V1;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V2;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@AllArgsConstructor
@Configuration
@SuppressWarnings("PMD.TooManyStaticImports")
public class OpenAPIConfig {

//    private final TypeResolver typeResolver;

    @Bean
    public OpenAPI ab2dAPI() {
        Server local = new Server();
        local.setUrl("https://localhost:8443/");

        return new OpenAPI()
                .addServersItem(local)
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .name("bearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .info(new Info().title("AB2D API"));
    }

    @Bean
    public GroupedOpenApi apiV1() {
        return GroupedOpenApi.builder()
                .group("V1 - FHIR STU3")
                .packagesToScan("gov.cms.ab2d.api.controller")
                .pathsToMatch(API_PREFIX_V1 + FHIR_PREFIX + "/**")
                .build();
    }

    @Bean
    public GroupedOpenApi apiV2() {
        return GroupedOpenApi.builder()
                .group("V2 - FHIR R4")
                .packagesToScan("gov.cms.ab2d.api.controller")
                .pathsToMatch(API_PREFIX_V2 + FHIR_PREFIX + "/**")
                .build();
    }

//    private List<ResponseMessage> globalResponseMessages() {
//        return Arrays.asList(new ResponseMessageBuilder()
//                .code(500)
//                .message(
//                        "An error occurred. " + Constants.GENERIC_FHIR_ERR_MSG)
//                .responseModel(new ModelRef("OperationOutcome"))
//                .build(), new ResponseMessageBuilder()
//                .code(400)
//                .message(
//                        "There was a problem with the request. " + Constants.GENERIC_FHIR_ERR_MSG)
//                .responseModel(new ModelRef("OperationOutcome"))
//                .build(), new ResponseMessageBuilder()
//                .code(401)
//                .message(
//                        "Unauthorized. Missing authentication token.")
//                .build(), new ResponseMessageBuilder()
//                .code(403)
//                .message(
//                        "Forbidden. Access not permitted.")
//                .build());
//    }
//
//    private ApiInfo apiInfoV1() {
//        return new ApiInfo(
//               "AB2D FHIR STU3 Bulk Data Access API",
//                MAIN,
//               "1.0",
//               null,
//               null,
//               null, null, Collections.emptyList());
//    }
//
//    private ApiInfo apiInfoV2() {
//        return new ApiInfo(
//                "AB2D FHIR R4 Bulk Data Access API",
//                MAIN,
//                "2.0",
//                null,
//                null,
//                null, null, Collections.emptyList());
//    }

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

//    private ApiKey apiKey() {
//        return new ApiKey(AUTHORIZATION, AUTHORIZATION, "header");
//    }
}
