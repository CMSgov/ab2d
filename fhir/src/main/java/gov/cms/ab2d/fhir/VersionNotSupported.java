package gov.cms.ab2d.fhir;

/**
 * Thrown when a version is not supported by the code
 */
public class VersionNotSupported extends RuntimeException {
    public VersionNotSupported(String msg) {
        super(msg);
    }
}
