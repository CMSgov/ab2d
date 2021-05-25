package gov.cms.ab2d.loadtest;

import gov.cms.ab2d.e2etest.APIClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import gov.cms.ab2d.fhir.FhirVersion;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.tuple.ImmutablePair;

@Slf4j
public class TestRunner extends AbstractJavaSamplerClient {
    private static final String FHIR_TYPE = "application/fhir+ndjson";

    private String[] contractArr;

    private APIClient apiClient;

    @Override
    public Arguments getDefaultParameters() {
        final Arguments arguments = new Arguments();
        arguments.addArgument("contracts", "Z0003");
        arguments.addArgument("api-url", "https://localhost:8080/api/v1/fhir/");
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
        } catch (IOException | InterruptedException | JSONException | NoSuchAlgorithmException | KeyManagementException e) {
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

        private SampleResult createContractResultAndStart() {
            SampleResult contractResult = new SampleResult();
            addSubResultToSampleResult(mainResult, contractResult);
            contractResult.sampleStart();
            contractResult.setThreadName("Contract " + contractNumber);

            return contractResult;
        }

        private SampleResult createExportResultAndStart(SampleResult contractResult) {
            SampleResult exportResult = new SampleResult();
            contractResult.addSubResult(exportResult);
            exportResult.setSuccessful(false);
            exportResult.setThreadName("Export for contract " + contractNumber);
            exportResult.sampleStart();

            return exportResult;
        }

        @Override
        public void run() {
            SampleResult contractResult = createContractResultAndStart();

            SampleResult exportResult = createExportResultAndStart(contractResult);

            try {
                HttpResponse<String> exportResponse = apiClient.exportByContractRequest(contractNumber, FHIR_TYPE, null, FhirVersion.STU3);

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

                SampleResult statusResult = new SampleResult();
                contractResult.addSubResult(statusResult);
                statusResult.setSuccessful(false);
                statusResult.setThreadName("Status checks for contract " + contractNumber);
                statusResult.sampleStart();

                ImmutablePair<HttpResponse<String>, Integer> statusResponses = performStatusCalls(url, statusResult);

                HttpResponse<String> statusResponse = statusResponses.getLeft();
                int status = statusResponses.getRight();

                statusResult.sampleEnd();
                statusResult.setResponseCode(String.valueOf(statusResponse.statusCode()));
                statusResult.setResponseMessage(statusResponse.body());

                checkStatusAndPerformDownloadResult(status, statusResult, contractResult, statusResponse);

                contractResult.setSuccessful(true);
            } catch (Exception e) {
                log.error("Exception occurred during execution of worker", e);
            } finally {
                contractResult.sampleEnd();
                countDownLatch.countDown();
            }
        }

        private void checkStatusAndPerformDownloadResult(int status, SampleResult statusResult, SampleResult contractResult,
                                                         HttpResponse<String> statusResponse) throws JSONException, IOException, InterruptedException {
            if (status == 200) {
                statusResult.setSuccessful(true);

                SampleResult downloadGroupResult = new SampleResult();
                contractResult.addSubResult(downloadGroupResult);
                downloadGroupResult.setSuccessful(false);
                downloadGroupResult.sampleStart();

                JSONObject json = new JSONObject(statusResponse.body());
                JSONArray output = json.getJSONArray("output");
                for (int j = 0; j < output.length(); j++) {
                    createDownloadResultsAndProcess(j, output, downloadGroupResult);
                }

                downloadGroupResult.sampleEnd();
                downloadGroupResult.setSuccessful(true);
            } else {
                throw new RuntimeException("Received error from server when checking status for contract " + contractNumber + " - " +
                        statusResponse.body());
            }
        }

        private ImmutablePair<HttpResponse<String>, Integer>  performStatusCalls(String url, SampleResult statusResult) throws InterruptedException, IOException {
            HttpResponse<String> statusResponse = null;
            int status = 0;
            boolean finishedStatus = false;
            int i = 1;
            while (!finishedStatus) {
                Thread.sleep(50);

                SampleResult statusResultCall = new SampleResult();
                statusResult.addSubResult(statusResultCall);
                statusResultCall.setSuccessful(false);
                statusResultCall.setThreadName("Status call " + i + " for contract " + contractNumber);
                statusResultCall.sampleStart();

                statusResponse = apiClient.statusRequest(url);
                status = statusResponse.statusCode();

                statusResultCall.sampleEnd();
                statusResultCall.setSuccessful(true);
                statusResultCall.setResponseCode(String.valueOf(status));

                if (status == 200 || status == 500) {
                    finishedStatus = true;
                }

                i++;
            }

            return ImmutablePair.of(statusResponse, status);
        }

        private void createDownloadResultsAndProcess(int index, JSONArray output, SampleResult downloadGroupResult) throws JSONException, IOException, InterruptedException {
            JSONObject outputObject = output.getJSONObject(index);
            String downloadUrl = outputObject.getString("url");

            SampleResult downloadResult = new SampleResult();
            downloadGroupResult.addSubResult(downloadResult);
            downloadResult.setSuccessful(false);
            downloadResult.sampleStart();
            downloadResult.setThreadName("Download for contract " + contractNumber + " with URL " + downloadUrl);

            HttpResponse<InputStream> downloadResponse = apiClient.fileDownloadRequest(downloadUrl);

            downloadResult.sampleEnd();
            downloadResult.setResponseCode(String.valueOf(downloadResponse.statusCode()));
            downloadResult.setBodySize((long) downloadResponse.body().readAllBytes().length);

            if (downloadResponse.statusCode() == 200) {
                downloadResult.setSuccessful(true);
            } else {
                throw new RuntimeException("Received error when trying to download file for contract " + contractNumber + " - " +
                        downloadResponse.body());
            }
        }
    }
}
