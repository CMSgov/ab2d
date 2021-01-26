package gov.cms.ab2d.fhir;

public class VersionNotSupported extends RuntimeException {
    public VersionNotSupported(String msg) {
        super(msg);
    }
}
