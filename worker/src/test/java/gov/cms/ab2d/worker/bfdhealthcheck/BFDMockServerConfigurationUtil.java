package gov.cms.ab2d.worker.bfdhealthcheck;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.ServerSocket;

import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;

public class BFDMockServerConfigurationUtil {

    public static final int MOCK_SERVER_PORT = randomMockServerPort();

    public static class PropertyOverrider implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            String baseUrl = "bfd.serverBaseUrlStu3=http://localhost:" + MOCK_SERVER_PORT + "/v1/fhir/";
            addInlinedPropertiesToEnvironment(applicationContext, baseUrl);
        }
    }

    private static int randomMockServerPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException ioException) {
            throw new RuntimeException("could not find open port");
        }
    }
}
