package gov.cms.ab2d.common.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PropertyConstants {

    private PropertyConstants() { }

    // Properties that are allowed to be modified. When adding a new one, add it to a constant, and the Set below
    public static final String PCP_CORE_POOL_SIZE = "pcp.core.pool.size";

    public static final String PCP_MAX_POOL_SIZE = "pcp.max.pool.size";

    public static final String PCP_SCALE_TO_MAX_TIME = "pcp.scaleToMax.time";

    public static final String MAINTENANCE_MODE = "maintenance.mode";

    // Accepted values: engaged, idle
    public static final String WORKER_ENGAGEMENT = "worker.engaged";

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
}
