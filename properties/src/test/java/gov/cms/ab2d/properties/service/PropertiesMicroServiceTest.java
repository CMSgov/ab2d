package gov.cms.ab2d.properties.service;

import gov.cms.ab2d.properties.dto.PropertiesDTO;
import gov.cms.ab2d.properties.utils.AB2DPostgresqlContainer;
import org.mockserver.model.Header;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.apache.http.HttpStatus;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "feature.property.service.enabled=true",
        "property.service.url=http://localhost:7177",
})
@Testcontainers
public class PropertiesMicroServiceTest {
    @Autowired
    private PropertiesAPIService propertiesAPIService;

    private static MockServerClient mockServer;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer("data-postgres.sql");

    private static final int PORT = 7177;

    private static final String NEW_ONE = "newone";

    @BeforeAll
    static void initServer() {
        mockServer = ClientAndServer.startClientAndServer(PORT);
        createMockServerExpectation("/properties", "GET", HttpStatus.SC_OK,
               "[{ \"key\": \"pcp.scaleToMax.time\", \"value\": \"400\"},{ \"key\": \"maintenance.mode\", \"value\": \"false\"},{ \"key\": \"propService\", \"value\": \"true\"}]");
        createMockServerExpectation("/properties/maintenance.mode", "GET", HttpStatus.SC_OK,
                "{ \"key\": \"maintenance.mode\", \"value\": \"false\"}");
        createMockServerExpectation("/properties/bogus", "GET", HttpStatus.SC_NOT_FOUND,
                "{ \"key\": \"null\", \"value\": \"null\"}");
        createMockServerExpectation("/properties/bogus", "GET", HttpStatus.SC_NOT_FOUND,
                "{ \"key\": \"null\", \"value\": \"null\"}");
        createMockServerExpectation("/properties", "POST", HttpStatus.SC_OK,
                "{ \"key\": \"newone\", \"value\": \"one\"}",
                List.of(Parameter.param("key", NEW_ONE), Parameter.param("value", "one")));
        createMockServerExpectation("/properties/newone", "DELETE", HttpStatus.SC_OK, "true");
        createMockServerExpectation("/properties", "POST", HttpStatus.SC_NOT_FOUND,
                "",
                List.of(Parameter.param("key", "second"), Parameter.param("value", "two")));
    }

    @Test
    void testProperties() {
        List<PropertiesDTO> properties = propertiesAPIService.getAllProperties();
        assertEquals(3, properties.size());
        String value = propertiesAPIService.getProperty("maintenance.mode");
        assertEquals("false", value);
        String value2 = propertiesAPIService.getProperty("bogus");
        assertNull(value2);
        assertTrue(propertiesAPIService.createProperty(NEW_ONE, "one"));
        assertFalse(propertiesAPIService.createProperty("", "one"));
        assertFalse(propertiesAPIService.createProperty(NEW_ONE, ""));
        assertFalse(propertiesAPIService.createProperty(NEW_ONE, null));
        assertFalse(propertiesAPIService.createProperty(null, "one"));
        assertFalse(propertiesAPIService.createProperty(null, null));
        assertTrue(propertiesAPIService.deleteProperty(NEW_ONE));
        assertTrue(propertiesAPIService.deleteProperty("pcp.core.pool.size"));
        assertTrue(propertiesAPIService.updateProperty(NEW_ONE, "two"));
        assertTrue(propertiesAPIService.createProperty("second", "two"));
    }

    static void createMockServerExpectation(String path, String verb, int respCode, String payload) {
        createMockServerExpectation(path, verb, respCode, payload, List.of());
    }

    static void createMockServerExpectation(String path, String verb, int respCode, String payload,
                                            List<Parameter> qStringParams) {
        new MockServerClient("localhost", PORT)
                .when(HttpRequest.request()
                                .withMethod(verb)
                                .withPath(path)
                                .withQueryStringParameters(qStringParams),
                        Times.unlimited()
                )
                .respond(org.mockserver.model.HttpResponse.response()
                        .withStatusCode(respCode)
                        .withHeader(new Header("Content-Type","application/json;charset=UTF-8"))
                        .withBody(payload)
                        .withDelay(TimeUnit.MILLISECONDS, 100)
                );
    }
}
