package gov.cms.ab2d.loadtest;

import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.util.List;

public class TestRunner extends AbstractJavaSamplerClient {

    List<String> contractNumbers;

    public TestRunner(List<String> contractNumbers, String clientSecret) {
        this.contractNumbers = contractNumbers;


    }

    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        final SampleResult sampleResult = new SampleResult();
        sampleResult.setSampleLabel("Load Test");
        sampleResult.setSuccessful(false);
        sampleResult.sampleStart();


        sampleResult.setSuccessful(true);
        return sampleResult;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {

    }
}
