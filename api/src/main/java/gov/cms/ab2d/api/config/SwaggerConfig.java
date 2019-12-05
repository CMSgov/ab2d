package gov.cms.ab2d.api.config;

import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import gov.cms.ab2d.api.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.*;

import static gov.cms.ab2d.api.util.Constants.API_PREFIX;
import static gov.cms.ab2d.api.util.Constants.FHIR_PREFIX;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Autowired
    private TypeResolver typeResolver;

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("gov.cms.ab2d.api.controller"))
                .paths(PathSelectors.ant(API_PREFIX + FHIR_PREFIX + "/**"))
                .build()
                .directModelSubstitute(Resource.class, String.class)
                .directModelSubstitute(JsonNode.class, Void.class)
                .useDefaultResponseMessages(false)
                .globalResponseMessage(RequestMethod.GET,
                        globalResponseMessages())
                .globalResponseMessage(RequestMethod.DELETE,
                        globalResponseMessages())
                .apiInfo(apiInfo()).additionalModels(typeResolver.resolve(OperationOutcome.class));
    }

    private List<ResponseMessage> globalResponseMessages() {
        return Arrays.asList(new ResponseMessageBuilder()
                .code(500)
                .message(
                        "An error occurred. " + Constants.GENERIC_FHIR_ERR_MSG)
                .responseModel(new ModelRef("OperationOutcome"))
                .build(), new ResponseMessageBuilder()
                .code(400)
                .message(
                        "There was a problem with the request. " + Constants.GENERIC_FHIR_ERR_MSG)
                .responseModel(new ModelRef("OperationOutcome"))
                .build(), new ResponseMessageBuilder()
                .code(401)
                .message(
                        "Unauthorized. Missing authentication token.")
                .build(), new ResponseMessageBuilder()
                .code(403)
                .message(
                        "Forbidden. Access not permitted.")
                .build());
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
                "AB2D FHIR Bulk Data Access API",
                "Provides Part A & B claim data to PDP sponsors.",
                "1.0",
                null,
                null,
                null, null, Collections.emptyList());
    }


    // FHIR's OperationOutcome won't play nice with Swagger. Having to redefine it here
    // to get the Swagger API spec looking right.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({
            "text"
    })
    class Details {

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
    class Issue {

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
