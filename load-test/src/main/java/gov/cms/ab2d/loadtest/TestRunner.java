package gov.cms.ab2d.loadtest;

import gov.cms.ab2d.common.httpclient.APIClient;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class TestRunner extends AbstractJavaSamplerClient {

    private String[] contractArr;

    private APIClient apiClient;

    @Override
    public Arguments getDefaultParameters() {
        final Arguments arguments = new Arguments();
        arguments.addArgument("contracts", "S0000");
        arguments.addArgument("api-url", "http://localhost:8080/api/v1/fhir/");
        arguments.addArgument("okta-url", "https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token");

        return arguments;
    }

    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        final SampleResult sampleResult = new SampleResult();
        sampleResult.setSampleLabel("Load Test");
        sampleResult.setSuccessful(false);
        sampleResult.sampleStart();

        CountDownLatch countDownLatch = new CountDownLatch(contractArr.length);
        for (String contract : contractArr) {
            WorkflowWorker workflowWorker = new WorkflowWorker(contract, countDownLatch);
            workflowWorker.start();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sampleResult.sampleEnd();
        sampleResult.setSuccessful(true);
        return sampleResult;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);

        String contracts = context.getParameter("contracts");
        contractArr = contracts.split(",");

        String oktaUrl = context.getParameter("okta-url");
        String oktaClientId = System.getenv("AB2D_OKTA_CLIENT_ID");
        String oktaClientPassword = System.getenv("AB2D_OKTA_CLIENT_PASSWORD");

        try {
            apiClient = new APIClient(context.getParameter("api-url"), oktaUrl, oktaClientId, oktaClientPassword);
        } catch (IOException | InterruptedException | JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {

    }

    class WorkflowWorker extends Thread {

        private final String contractNumber;

        private final CountDownLatch countDownLatch;

        WorkflowWorker(String contractNumber, CountDownLatch countDownLatch) {
            this.contractNumber = contractNumber;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            try {
                apiClient.exportByContractRequest(contractNumber);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            countDownLatch.countDown();
        }
    }
}
