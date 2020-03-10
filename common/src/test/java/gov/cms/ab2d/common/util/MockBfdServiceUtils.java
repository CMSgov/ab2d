package gov.cms.ab2d.common.util;

import org.apache.http.HttpStatus;
import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.TimeUnit;

public class MockBfdServiceUtils {

    public static void reset(int mockServerPort) {
        new MockServerClient("localhost", mockServerPort).reset();
    }

    static void createMockServerExpectation(String path, int respCode, String payload,
                                                    List<Parameter> qStringParams, int delayMs, int mockServerPort) {
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

    public static void createMockServerExpectation(String path, int respCode, String payload,
                                                    List<Parameter> qStringParams, int port) {
        var delay = 100;
        createMockServerExpectation(path, respCode, payload, qStringParams, delay, port);
    }

    public static String getRawXML(String path) throws IOException {
        InputStream sampleData = MockBfdServiceUtils.class.getClassLoader().getResourceAsStream(path);
        if (sampleData == null) {
            throw new MissingResourceException("Cannot find sample requests",
                    MockBfdServiceUtils.class.getName(), path);
        }
        return new String(sampleData.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static void createMockServerMetaExpectation(String locOfFile, int mockServerPort) throws IOException {
        createMockServerExpectation("/v1/fhir/metadata",
                200,
                getRawXML(locOfFile),
                List.of(),
                mockServerPort);
    }

    public static void createMockServerPatientExpectation(String locOfFile, int mockServerPort, List<Parameter> parms) throws IOException {
        MockBfdServiceUtils.createMockServerExpectation(
                "/v1/fhir/Patient",
                HttpStatus.SC_OK,
                MockBfdServiceUtils.getRawXML(locOfFile),
                parms, mockServerPort);
    }
}
