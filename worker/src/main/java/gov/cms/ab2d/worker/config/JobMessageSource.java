package gov.cms.ab2d.worker.config;

import org.springframework.integration.jdbc.JdbcPollingChannelAdapter;

import javax.sql.DataSource;

/**
 * Extends the JdbcPollingChannelAdapter to implement a Job table specific polling Adapter.
 *
 * Two kinds of jobs are eligible:
 *      Submitted jobs that no worker currently holds a lock for.
 *      In-progress jobs with a stale (dead) lease, for hard-crash recovery.
 */
public class JobMessageSource extends JdbcPollingChannelAdapter {

    // Checks in-progress jobs only if the prototype is enabled, and only if those jobs have a lease row.
    // Jobs without a lease row are started by the normal worker, and are ignored.
    private static final String QUERY_TEMPLATE =
    """
    SELECT id, job_uuid, status, contract_number, fhir_version
    FROM job
    WHERE (
            (
                status = 'SUBMITTED'
                AND NOT EXISTS (SELECT 1 FROM int_lock WHERE lock_key = job.job_uuid)
            )
            OR (
                status = 'IN_PROGRESS'
                AND EXISTS (SELECT 1 FROM property.properties
                            WHERE key = 'pause-resume.prototype.enabled' AND value = 'true')
                AND EXISTS (
                    SELECT 1 FROM ab2d.job_lease l
                    WHERE l.job_uuid = job.job_uuid
                      AND l.heartbeat_at < now() - make_interval(secs => %d)
                )
            )
          )
    ORDER BY created_at;
    """;

    public JobMessageSource(DataSource dataSource, int leaseTtlSeconds) {
        super(dataSource, buildQuery(leaseTtlSeconds));
        setMaxRows(10);
    }

    /**
     * Exposes the query so that tests can use it
     */
    static String buildQuery(int leaseTtlSeconds) {
        return String.format(QUERY_TEMPLATE, leaseTtlSeconds);
    }
}
