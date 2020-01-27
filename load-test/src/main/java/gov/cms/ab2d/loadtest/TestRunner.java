package gov.cms.ab2d.loadtest;

import gov.cms.ab2d.common.httpclient.APIClient;
import gov.cms.ab2d.common.util.JobUtil;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.json.JSONException;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class TestRunner extends AbstractJavaSamplerClient {

    private String[] contractArr;

    private static final int TRIES_BEFORE_BACKOFF = 100000;

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
            String apiUrl = javaSamplerContext.getParameter("api-url");
            String oktaUrl = javaSamplerContext.getParameter("okta-url");
            String oktaClientId = System.getenv("AB2D_OKTA_CLIENT_ID");
            String oktaClientPassword = System.getenv("AB2D_OKTA_CLIENT_PASSWORD");

            APIClient apiClient = null;
            try {
                apiClient = new APIClient(apiUrl, oktaUrl, oktaClientId, oktaClientPassword);
            } catch (IOException | InterruptedException | JSONException e) {
                throw new RuntimeException(e);
            }
            WorkflowWorker workflowWorker = new WorkflowWorker(contract, countDownLatch, apiClient, sampleResult);
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

        System.out.println("here");
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {

    }

    class WorkflowWorker extends Thread {

        private final String contractNumber;

        private final CountDownLatch countDownLatch;

        private final APIClient apiClient;

        private final SampleResult mainResult;

        WorkflowWorker(String contractNumber, CountDownLatch countDownLatch, APIClient apiClient, SampleResult mainResult) {
            this.contractNumber = contractNumber;
            this.countDownLatch = countDownLatch;
            this.apiClient = apiClient;
            this.mainResult = mainResult;
        }

        @Override
        public void run() {
            SampleResult exportResult = new SampleResult();
            exportResult.setSuccessful(false);
            exportResult.sampleStart();
            exportResult.setSampleLabel("Export for contract " + contractNumber);

            try {
                HttpResponse<String> exportResponse = apiClient.exportByContractRequest(contractNumber);

                if(exportResponse.statusCode() == 202) {
                    exportResult.setSuccessful(true);
                    exportResult.sampleEnd();
                    addSubResultToSampleResult(mainResult, exportResult);
                } else {
                    exportResult.setResponseMessage(exportResponse.body());
                    exportResult.sampleEnd();
                    addSubResultToSampleResult(mainResult, exportResult);
                    throw new RuntimeException("Encountered error when exporting for contract " + contractNumber);
                }

                List<String> contentLocationList = exportResponse.headers().map().get("content-location");
                String url = contentLocationList.get(0);

                int status = 0;
                HttpResponse<String> statusResponse = null;

                SampleResult statusResult = new SampleResult();
                statusResult.setSuccessful(false);
                statusResult.sampleStart();
                statusResult.setSampleLabel("Status check for contract " + contractNumber);

                // Eventually back off, otherwise the server will infinitely send a 429
                int i = 0;
                int delay = 50;
                boolean finishedStatus = false;
                while(!finishedStatus) {
                    Thread.sleep(delay);
                    statusResponse = apiClient.statusRequest(url);
                    status = statusResponse.statusCode();

                    if(status == 200 || status == 500) {
                        finishedStatus = true;
                    }

                    if(i > TRIES_BEFORE_BACKOFF) {
                        delay = 31000;
                    }
                    i++;
                }


                System.out.println(contractNumber + " -- Finishing with job " + contentLocationList.iterator().next());
                if(status == 200) {
                    statusResult.setSuccessful(true);
                    statusResult.sampleEnd();
                    addSubResultToSampleResult(mainResult, statusResult);

                    String jobUuid = JobUtil.getJobUuid(contentLocationList.iterator().next());

                    SampleResult downloadResult = new SampleResult();
                    downloadResult.setSuccessful(false);
                    downloadResult.sampleStart();
                    downloadResult.setSampleLabel("Download for contract " + contractNumber);

                    HttpResponse<String> downloadResponse = apiClient.fileDownloadRequest(jobUuid, contractNumber + ".ndjson");
                    if(downloadResponse.statusCode() == 200) {
                        downloadResult.setSuccessful(true);
                        downloadResult.sampleEnd();
                        addSubResultToSampleResult(mainResult, downloadResult);
                    } else {
                        downloadResult.setResponseMessage(downloadResponse.body());
                        downloadResult.sampleEnd();
                        addSubResultToSampleResult(mainResult, downloadResult);
                        throw new RuntimeException("Received error when trying to download file for contract " + contractNumber);
                    }
                } else {
                    statusResult.setResponseMessage(statusResponse.body());
                    statusResult.sampleEnd();
                    addSubResultToSampleResult(mainResult, statusResult);
                    throw new RuntimeException("Received error from server when checking status for contract " + contractNumber);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                countDownLatch.countDown();
            }
        }
    }
}
