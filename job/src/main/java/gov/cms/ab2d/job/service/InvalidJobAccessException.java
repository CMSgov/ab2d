package gov.cms.ab2d.job.service;

public class InvalidJobAccessException extends RuntimeException {

    public InvalidJobAccessException(String msg) {
        super(msg);
    }
}
