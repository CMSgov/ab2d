package gov.cms.ab2d.common.util;

import java.util.Set;

public final class Constants {

    private Constants() { }

    public static final String OPERATION_OUTCOME = "OperationOutcome";

    public static final String NDJSON_FIRE_CONTENT_TYPE = "application/fhir+ndjson";

    public static final String JOB_LOG = "job";

    public static final String FILE_LOG = "filename";

    public static final String CONTRACT_LOG = "contract";

    public static final String SPONSOR_LOG = "hpmsId";

    public static final String SPONSOR_ROLE = "SPONSOR";

    public static final String ADMIN_ROLE = "ADMIN";

    public static final String EOB = "ExplanationOfBenefit";

    public static final int ONE_MEGA_BYTE = 1024 * 1024;

    public static final String API_PREFIX = "/api/v1";

    public static final String FHIR_PREFIX = "/fhir";

    public static final String ADMIN_PREFIX = "/admin";

    // Properties that are allowed to be modified. When adding a new one, add it to a constant, and the Set below
    public static final String PCP_CORE_POOL_SIZE = "pcp.core.pool.size";

    public static final String PCP_MAX_POOL_SIZE = "pcp.max.pool.size";

    public static final String PCP_SCALE_TO_MAX_TIME = "pcp.scaleToMax.time";

    public static final String MAINTENANCE_MODE = "maintenance.mode";

    public static final Set<String> ALLOWED_PROPERTY_NAMES = Set.of(PCP_CORE_POOL_SIZE, PCP_MAX_POOL_SIZE,
            PCP_SCALE_TO_MAX_TIME, MAINTENANCE_MODE);
}
