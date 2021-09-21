package gov.cms.ab2d.bfd.client;

import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;

public class MockUtils {

    public static String KEYSTORE_FILE;
    public static String MITM_KEY;
    public static String MITM_PEM;

    static {
        try {
            Path tempDir = Files.createTempDirectory(RandomStringUtils.random(10, true, true));
            Path keystorePath = Files.copy(Paths.get(ClassLoader.getSystemResource("bb.keystore").toURI()),
                    Paths.get(tempDir.toString(), "bb.keystore"), StandardCopyOption.REPLACE_EXISTING);

            Path mitmKeyPath = Files.copy(Paths.get(ClassLoader.getSystemResource("mitm_bfd_cert.key").toURI()),
                    Paths.get(tempDir.toString(), "mitm_bfd_cert.key"), StandardCopyOption.REPLACE_EXISTING);

            Path mitmPemPath = Files.copy(Paths.get(ClassLoader.getSystemResource("mitm_bfd_cert.pem").toURI()),
                    Paths.get(tempDir.toString(), "mitm_bfd_cert.pem"), StandardCopyOption.REPLACE_EXISTING);

            KEYSTORE_FILE = keystorePath.toFile().getAbsolutePath();
            MITM_KEY = mitmKeyPath.toFile().getAbsolutePath();
            MITM_PEM = mitmPemPath.toFile().getAbsolutePath();

        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static class PropertyOverrider implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            String baseUrl = "bfd.keystore.location=" + KEYSTORE_FILE;
            addInlinedPropertiesToEnvironment(applicationContext, baseUrl);
        }
    }

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

    static void createKeystoreFile(Path tempDir) {

    }
}
