package gov.cms.ab2d.worker.config;

import org.springframework.integration.jdbc.JdbcPollingChannelAdapter;

import javax.sql.DataSource;

/**
 * Extends the JdbcPollingChannelAdapter to implement a Job table specific polling Adapter.
 *
 * Two kinds of jobs are eligible:
 *      Any submitted job
 *      In-progress jobs with no live lease, for hard-crash recovery.
 */
public class JobMessageSource extends JdbcPollingChannelAdapter {

    private static final String QUERY_TEMPLATE =
    """
    SELECT id, job_uuid, status, contract_number, fhir_version
    FROM job
    WHERE (
            status = 'SUBMITTED'
            OR (
                status = 'IN_PROGRESS'
                AND EXISTS (SELECT 1 FROM property.properties
                            WHERE key = 'pause-resume.prototype.enabled' AND value = 'true')
                AND NOT EXISTS (
                    SELECT 1 FROM ab2d.job_lease l
                    WHERE l.job_uuid = job.job_uuid
                      AND l.heartbeat_at >= now() - make_interval(secs => %d)
                )
            )
          )
    ORDER BY created_at;
    """;

    public JobMessageSource(DataSource dataSource, int leaseTtlSeconds) {
        super(dataSource, String.format(QUERY_TEMPLATE, leaseTtlSeconds));
        setMaxRows(10);
    }
}
