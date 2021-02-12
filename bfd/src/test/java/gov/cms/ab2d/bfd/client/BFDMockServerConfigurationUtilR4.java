package gov.cms.ab2d.bfd.client;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;

public class BFDMockServerConfigurationUtilR4 {

    public static final int MOCK_SERVER_PORT = 8080;

    public static class PropertyOverrider implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            String baseUrl = "bfd.serverBaseUrl=http://localhost:" + MOCK_SERVER_PORT + "/v2/fhir/";
            addInlinedPropertiesToEnvironment(applicationContext, baseUrl);
        }
    }
}
