package gov.cms.ab2d.api.controller;

import java.util.ArrayList;
import java.util.List;

/**
 * A POJO (that eventually gets serialized into JSON) to hold a "Response - Complete Status"
 * information
 * as per the Spec.
 */
public final class JobCompletedResponse {


    private String transactionTime;
    private String request;
    private boolean requiresAccessToken;
    private List<Output> output = new ArrayList<>();
    private List<Output> error = new ArrayList<>();


    public String getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public boolean isRequiresAccessToken() {
        return requiresAccessToken;
    }

    public void setRequiresAccessToken(boolean requiresAccessToken) {
        this.requiresAccessToken = requiresAccessToken;
    }

    public List<Output> getOutput() {
        return output;
    }

    public void setOutput(List<Output> output) {
        this.output = output;
    }

    public List<Output> getError() {
        return error;
    }

    public void setError(List<Output> error) {
        this.error = error;
    }

    public static final class Output {
        private String type;
        private String url;

        public Output(String type, String url) {
            this.type = type;
            this.url = url;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

}
