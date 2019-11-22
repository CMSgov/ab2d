package gov.cms.ab2d.api.util;

public final class Constants {

    public static final String GENERIC_FHIR_ERR_MSG = "The body will contain a FHIR " +
            "OperationOutcome resource in JSON format. " +
            "https://www.hl7.org/fhir/operationoutcome.html " +
            "Please refer to the body of the response for " +
            "details.";

    public static final String API_PREFIX = "/api/v1";

    public static final String FHIR_PREFIX = "/fhir";

    public static final String ADMIN_PREFIX = "/admin";

    public static final String SPONSOR_ROLE = "SPONSOR";

    public static final String ADMIN_ROLE = "ADMIN";

    private Constants() {
    }


}
