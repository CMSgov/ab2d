package gov.cms.ab2d.bfd.client;

import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MockUtils {
    static String getRawJson(String path) throws IOException {
        InputStream sampleData =
                BlueButtonClientTestR4.class.getClassLoader().getResourceAsStream(path);

        if (sampleData == null) {
            throw new IOException("Cannot find sample requests for path " + path);
        }

        return new String(sampleData.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Helper method that configures the mock server to respond to a given GET request
     *
     * @param path          The path segment of the URL that would be received by BlueButton
     * @param respCode      The desired HTTP response code
     * @param payload       The data that the mock server should return in response to this GET
     *                      request
     * @param qStringParams The query string parameters that must be present to generate this
     *                      response
     */
    static void createMockServerExpectation(String path, int respCode, String payload,
                                                    List<Parameter> qStringParams, int port) {
        var delay = 100;
        createMockServerExpectation(path, respCode, payload, qStringParams, delay, port);
    }

    static void createMockServerExpectation(String path, int respCode, String payload,
                                            List<Parameter> qStringParams, int delayMs, int port) {
        new MockServerClient("localhost", port)
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
                                                "application/json;charset=UTF-8")
                                )
                                .withBody(payload)
                                .withDelay(TimeUnit.MILLISECONDS, delayMs)
                );
    }

    static int randomMockServerPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException ioException) {
            throw new RuntimeException("could not find open port");
        }
    }
}
