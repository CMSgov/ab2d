package gov.cms.ab2d.common.util;

public final class Constants {

    private Constants() { }

    public static final String OPERATION_OUTCOME = "OperationOutcome";

    public static final String NDJSON_FIRE_CONTENT_TYPE = "application/fhir+ndjson";

    public static final String JOB_LOG = "job";

    public static final String ORGANIZATION = "organization";

    public static final String REQUEST_ID = "RequestId";

    public static final String FILE_LOG = "filename";

    public static final String CONTRACT_LOG = "contract";

    public static final String API_PREFIX_V1 = "/api/v1";

    public static final String API_PREFIX_V2 = "/api/v2";

    public static final String FHIR_PREFIX = "/fhir";

    public static final String ADMIN_PREFIX = "/admin";

    public static final String STATUS_ENDPOINT = "/status";

    public static final String HEALTH_ENDPOINT = "/health";

    public static final String VALIDATE_BFD_ENDPOINT = "/validate/bfd";

    public static final String VALIDATE_SLACK_ENDPOINT = "/validate/slack";

    public static final String AKAMAI_TEST_OBJECT = "/akamai-test-object.html";

    // Properties that are allowed to be modified. When adding a new one, add it to a constant, and the Set below
    public static final String ZIP_SUPPORT_ON = "ZipSupportOn";

    // This is the earliest time the _since filter is valid - probably should be in the properties file but I
    // wanted to include it in the swagger documentation and for the swagger annotation, the value has to be
    // constant at compile time so I put it here.
    public static final String SINCE_EARLIEST_DATE = "2020-02-13T00:00:00.000-05:00";

    public static final String ZIPFORMAT = "application/zip";
}
