package gov.cms.ab2d.coveragecounts;

public class CoverageCountException extends RuntimeException {
    public CoverageCountException(Exception ex) {
        super(ex);
    }
}
