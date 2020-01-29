package gov.cms.ab2d.loadtest;

import gov.cms.ab2d.common.httpclient.APIClient;
import gov.cms.ab2d.common.util.JobUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.json.JSONException;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class TestRunner extends AbstractJavaSamplerClient {

    private String[] contractArr;

    private static final int TRIES_BEFORE_BACKOFF = 100000;

    private APIClient apiClient;

    @Override
    public Arguments getDefaultParameters() {
        final Arguments arguments = new Arguments();
        arguments.addArgument("contracts", "S0003");
        arguments.addArgument("api-url", "http://localhost:8080/api/v1/fhir/");
        arguments.addArgument("okta-url", "https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token");

        return arguments;
    }

    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        String apiUrl = javaSamplerContext.getParameter("api-url");
        String oktaUrl = javaSamplerContext.getParameter("okta-url");
        String oktaClientId = System.getenv("AB2D_OKTA_CLIENT_ID");
        String oktaClientPassword = System.getenv("AB2D_OKTA_CLIENT_PASSWORD");
        try {
            apiClient = new APIClient(apiUrl, oktaUrl, oktaClientId, oktaClientPassword);
        } catch (IOException | InterruptedException | JSONException e) {
            throw new RuntimeException(e);
        }

        final SampleResult sampleResult = new SampleResult();
        sampleResult.setSampleLabel("Load Test");
        sampleResult.setSuccessful(false);
        sampleResult.sampleStart();

        CountDownLatch countDownLatch = new CountDownLatch(contractArr.length);
        for (String contract : contractArr) {
            WorkflowWorker workflowWorker = new WorkflowWorker(contract, countDownLatch, sampleResult);
            workflowWorker.start();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        sampleResult.setSuccessful(true);
        return sampleResult;
    }

    private synchronized void addSubResultToSampleResult(SampleResult sampleResult, SampleResult subResult) {
        sampleResult.addSubResult(subResult);
    }

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);

        String contracts = context.getParameter("contracts");
        contractArr = contracts.split(",(\\s)*");
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {

    }

    class WorkflowWorker extends Thread {

        private final String contractNumber;

        private final CountDownLatch countDownLatch;

        private final SampleResult mainResult;

        WorkflowWorker(String contractNumber, CountDownLatch countDownLatch, SampleResult mainResult) {
            this.contractNumber = contractNumber;
            this.countDownLatch = countDownLatch;
            this.mainResult = mainResult;
        }

        @Override
        public void run() {
            SampleResult contractResult = new SampleResult();
            addSubResultToSampleResult(mainResult, contractResult);
            contractResult.sampleStart();
            contractResult.setThreadName("Contract " + contractNumber);

            SampleResult exportResult = new SampleResult();
            contractResult.addSubResult(exportResult);
            exportResult.setSuccessful(false);
            exportResult.setThreadName("Export for contract " + contractNumber);
            exportResult.sampleStart();

            try {
                HttpResponse<String> exportResponse = apiClient.exportByContractRequest(contractNumber);

                exportResult.sampleEnd();
                exportResult.setResponseCode(String.valueOf(exportResponse.statusCode()));
                exportResult.setResponseMessage(exportResponse.body());

                if (exportResponse.statusCode() == 202) {
                    exportResult.setSuccessful(true);
                } else {
                    throw new RuntimeException("Encountered error when exporting for contract " + contractNumber);
                }

                List<String> contentLocationList = exportResponse.headers().map().get("content-location");
                String url = contentLocationList.get(0);

                int status = 0;
                HttpResponse<String> statusResponse = null;

                SampleResult statusResult = new SampleResult();
                contractResult.addSubResult(statusResult);
                statusResult.setSuccessful(false);
                statusResult.setThreadName("Status check for contract " + contractNumber);
                statusResult.sampleStart();

                // Eventually back off, otherwise the server will infinitely send a 429
                int i = 0;
                int delay = 50;
                boolean finishedStatus = false;
                while (!finishedStatus) {
                    Thread.sleep(delay);
                    statusResponse = apiClient.statusRequest(url);
                    status = statusResponse.statusCode();

                    if (status == 200 || status == 500) {
                        finishedStatus = true;
                    }

                    if (i > TRIES_BEFORE_BACKOFF) {
                        delay = 31000;
                    }
                    i++;
                }

                statusResult.sampleEnd();
                statusResult.setResponseCode(String.valueOf(statusResponse.statusCode()));
                statusResult.setResponseMessage(statusResponse.body());

                if (status == 200) {
                    statusResult.setSuccessful(true);

                    String jobUuid = JobUtil.getJobUuid(contentLocationList.iterator().next());

                    SampleResult downloadResult = new SampleResult();
                    contractResult.addSubResult(downloadResult);
                    downloadResult.setSuccessful(false);
                    downloadResult.sampleStart();
                    downloadResult.setThreadName("Download for contract " + contractNumber);

                    HttpResponse<String> downloadResponse = apiClient.fileDownloadRequest(jobUuid, contractNumber + ".ndjson");

                    downloadResult.sampleEnd();
                    downloadResult.setResponseCode(String.valueOf(downloadResponse.statusCode()));
                    downloadResult.setBodySize((long) downloadResponse.body().length());

                    if (downloadResponse.statusCode() == 200) {
                        downloadResult.setSuccessful(true);
                    } else {
                        throw new RuntimeException("Received error when trying to download file for contract " + contractNumber + " - " +
                                downloadResponse.body());
                    }
                } else {
                    throw new RuntimeException("Received error from server when checking status for contract " + contractNumber + " - " +
                            statusResponse.body());
                }

                contractResult.setSuccessful(true);
            } catch (Exception e) {
                log.error("Exception occurred during execution of worker", e);
            } finally {
                contractResult.sampleEnd();
                countDownLatch.countDown();
            }
        }
    }
}
