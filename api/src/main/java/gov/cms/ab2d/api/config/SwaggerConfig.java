package gov.cms.ab2d.api.config;

import gov.cms.ab2d.api.util.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static gov.cms.ab2d.api.util.Constants.API_PREFIX;
import static gov.cms.ab2d.api.util.Constants.FHIR_PREFIX;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("gov.cms.ab2d.api.controller"))
                .paths(PathSelectors.ant(API_PREFIX + FHIR_PREFIX+"/**"))
                .build()
                .directModelSubstitute(Resource.class, String.class)
                .useDefaultResponseMessages(false)
                .globalResponseMessage(RequestMethod.GET,
                        globalResponseMessages())
                .globalResponseMessage(RequestMethod.DELETE,
                        globalResponseMessages())
                .apiInfo(apiInfo());
    }

    private List<ResponseMessage> globalResponseMessages() {
        return Arrays.asList(new ResponseMessageBuilder()
                .code(500)
                .message(
                        "An error occurred. " + Constants.GENERIC_FHIR_ERR_MSG)
                .build(), new ResponseMessageBuilder()
                .code(400)
                .message(
                        "There was a problem with the request. " + Constants.GENERIC_FHIR_ERR_MSG)
                .build(), new ResponseMessageBuilder()
                .code(401)
                .message(
                        "Unauthorized. " + Constants.GENERIC_FHIR_ERR_MSG)
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
}
