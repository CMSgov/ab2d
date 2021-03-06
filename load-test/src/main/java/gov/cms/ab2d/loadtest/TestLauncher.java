package gov.cms.ab2d.loadtest;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

// Class for running/testing code w/o using the JMeter GUI or command line tools, useful for quick prototyping
public class TestLauncher {

    public static void main(String[] args) {
        TestRunner testRunner = new TestRunner();
        Arguments arguments = new Arguments();
        arguments.addArgument("contracts", "Z0003");
        arguments.addArgument("api-url", "https://localhost:8080/api/v1/fhir/");
        arguments.addArgument("okta-url", "https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token");
        JavaSamplerContext javaSamplerContext = new JavaSamplerContext(arguments);
        testRunner.setupTest(javaSamplerContext);
        testRunner.runTest(javaSamplerContext);
    }
}
