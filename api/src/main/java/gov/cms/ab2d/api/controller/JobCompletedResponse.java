package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.apache.commons.codec.binary.Hex;

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

    public static final String CHECKSUM_STRING = "https://ab2d.cms.gov/checksum";
    public static final String CONTENT_LENGTH_STRING = "https://ab2d.cms.gov/file_length";


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

    @Getter
    public static final class Output {
        private final String type;
        private final String url;

        private final List<FileMetadata> extension;

        public Output(String type, String url, List<FileMetadata> valueOutputs) {
            this.type = type;
            this.url = url;
            this.extension = valueOutputs;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Getter
    public static final class FileMetadata {
        private final String url;

        private String valueString;

        private Long valueDecimal;

        public FileMetadata(byte[] valueString) {
            this.url = CHECKSUM_STRING;
            String stringChecksum = Hex.encodeHexString(valueString);

            String formattedChecksum = String.format("%s:%s", "sha256", stringChecksum);
            this.valueString = formattedChecksum;
        }

        public FileMetadata(Long valueDecimal) {
            this.url = CONTENT_LENGTH_STRING;
            this.valueDecimal = valueDecimal;
        }
    }
}
