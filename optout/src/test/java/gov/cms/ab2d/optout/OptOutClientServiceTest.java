package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.repository.OptOutRepository;
import org.apache.http.HttpStatus;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class)
class OptOutClientServiceTest {
    @Autowired
    private OptOutConverterService cut;
    @Autowired
    private OptOutRepository optOutRepository;

    private static int mockServerPort = 8083;

    private static ClientAndServer mockServer;

    private static final String TEST_DIR = "test-data/";

    @BeforeAll
    public static void setupBFDClient() throws IOException {
        mockServer = ClientAndServer.startClientAndServer(mockServerPort);

        createMockServerExpectation("/v1/fhir/metadata",
                200,
                getRawXML("test-data/meta.xml"),
                List.of());

        createMockServerExpectation(
                "/v1/fhir/Patient",
                HttpStatus.SC_OK,
                getRawXML(TEST_DIR + "patientbundle.xml"),
                List.of()
        );
    }

    @AfterAll
    public static void tearDown() {
        mockServer.stop();
    }

    @Test
    public void getOptOut() {
        final String line = getLinesFromFile().skip(6).limit(1).collect(Collectors.toList()).get(0);
        final List<OptOut> optOut = cut.convert(line);
        assertNotNull(optOut);
        assertEquals(2, optOut.size());
        assertEquals("20010000001115", optOut.get(0).getCcwId());
        optOut.forEach(o -> optOutRepository.save(o));
    }

    private Stream<String> getLinesFromFile() {
        final String testInputFile = "test-data/test-data.txt";
        final InputStream inputStream = getClass().getResourceAsStream("/" + testInputFile);
        final InputStreamReader isr = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(isr);
        return bufferedReader.lines();
    }

    private static void createMockServerExpectation(String path, int respCode, String payload,
                                                    List<Parameter> qStringParams) {
        var delay = 100;
        createMockServerExpectation(path, respCode, payload, qStringParams, delay);
    }

    private static void createMockServerExpectation(String path, int respCode, String payload,
                                                    List<Parameter> qStringParams, int delayMs) {
        new MockServerClient("localhost", mockServerPort)
                .when(
                        HttpRequest.request()
                                .withMethod("GET")
                                .withPath(path)
                                .withQueryStringParameters(qStringParams),
                        Times.unlimited()
                )
                .respond(
                        org.mockserver.model.HttpResponse.response()
                                .withStatusCode(respCode)
                                .withHeader(
                                        new Header("Content-Type",
                                                "application/fhir+xml;charset=UTF-8")
                                )
                                .withBody(payload)
                                .withDelay(TimeUnit.MILLISECONDS, delayMs)
                );
    }

    private static String getRawXML(String path) throws IOException {
        InputStream sampleData = OptOutClientServiceTest.class.getClassLoader().getResourceAsStream(path);
        if (sampleData == null) {
            throw new MissingResourceException("Cannot find sample requests",
                    OptOutClientServiceTest.class.getName(), path);
        }
        return new String(sampleData.readAllBytes(), StandardCharsets.UTF_8);
    }
}