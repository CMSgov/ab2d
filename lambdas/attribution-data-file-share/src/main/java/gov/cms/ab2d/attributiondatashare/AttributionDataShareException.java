package gov.cms.ab2d.attributiondatashare;

public class AttributionDataShareException extends RuntimeException {
    public AttributionDataShareException(String message, Exception ex) {
        super(message, ex);
    }
}
