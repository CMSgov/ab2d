package gov.cms.ab2d.api.controller;
import lombok.Getter;
import java.util.List;

@Getter
public class TooManyRequestsException extends RuntimeException {
    private List<String> jobIds;

    public TooManyRequestsException(String msg) {
        super(msg);
    }

    public TooManyRequestsException(String msg, List<String> jobIds) {
        super(msg);
        this.jobIds = jobIds;
    }
}
