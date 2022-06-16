package gov.cms.ab2d.common.util;

import java.util.Set;

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
    public static final String PCP_CORE_POOL_SIZE = "pcp.core.pool.size";

    public static final String PCP_MAX_POOL_SIZE = "pcp.max.pool.size";

    public static final String PCP_SCALE_TO_MAX_TIME = "pcp.scaleToMax.time";

    public static final String MAINTENANCE_MODE = "maintenance.mode";

    // Accepted values: engaged, idle
    public static final String WORKER_ENGAGEMENT = "worker.engaged";

    // Accepted values: engaged, idle
    public static final String SNS_JOB_UPDATE_ENGAGEMENT = "sns_job_update.engaged";

    // Accepted values: engaged, idle
    public static final String HPMS_INGESTION_ENGAGEMENT = "hpms.ingest.engaged";

    // Control when automatic metadata loading is and isn't enabled
    public static final String COVERAGE_SEARCH_DISCOVERY = "coverage.update.discovery";
    public static final String COVERAGE_SEARCH_QUEUEING = "coverage.update.queueing";

    // How many hours a coverage search can run before it times out
    public static final String COVERAGE_SEARCH_STUCK_HOURS = "coverage.update.stuck.hours";

    // How many months in the past coverage searches of BFD need to be done for. All coverage
    // periods for all active contracts within COVERAGE_SEARCH_UPDATE_MONTHS will have coverage
    // searches queued to update them whenever the coverage period quartz job runs.
    public static final String COVERAGE_SEARCH_UPDATE_MONTHS = "coverage.update.months.past";

    // Force a coverage update to run even when not scheduled
    public static final String COVERAGE_SEARCH_OVERRIDE = "coverage.update.override";

    public static final String ZIP_SUPPORT_ON = "ZipSupportOn";

    public static final Set<String> ALLOWED_PROPERTY_NAMES = Set.of(PCP_CORE_POOL_SIZE, PCP_MAX_POOL_SIZE,
            PCP_SCALE_TO_MAX_TIME, MAINTENANCE_MODE, ZIP_SUPPORT_ON,
            WORKER_ENGAGEMENT, HPMS_INGESTION_ENGAGEMENT, COVERAGE_SEARCH_DISCOVERY, COVERAGE_SEARCH_QUEUEING,
            COVERAGE_SEARCH_STUCK_HOURS, COVERAGE_SEARCH_UPDATE_MONTHS, COVERAGE_SEARCH_OVERRIDE, SNS_JOB_UPDATE_ENGAGEMENT);

    // This is the earliest time the _since filter is valid - probably should be in the properties file but I
    // wanted to include it in the swagger documentation and for the swagger annotation, the value has to be
    // constant at compile time so I put it here.
    public static final String SINCE_EARLIEST_DATE = "2020-02-13T00:00:00.000-05:00";

    public static final String ZIPFORMAT = "application/zip";

    public static final String SNS_TOPIC_AB2D_JOB_TRACKING = "ab2d-job-tracking";

    public static final String SQS_QUEUE_AB2D_JOB_TRACKING = "ab2d-job-tracking";
}
