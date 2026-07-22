package gov.cms.ab2d.worker.config;

import org.springframework.integration.jdbc.JdbcPollingChannelAdapter;

import javax.sql.DataSource;

/**
 * Extends the JdbcPollingChannelAdapter to implement a Job table specific polling Adapter.
 * The SQL query is specified as a static constant.
 */
public class JobMessageSource extends JdbcPollingChannelAdapter {

    // eligible jobs are either SUBMITTED or they are IN_PROGRESS with an expired lock
    private static final String QUERY_GET_NEXT_UNPROCESSED_JOB =
    """
    SELECT id, job_uuid, status, contract_number, fhir_version
    FROM job
    WHERE (
            status = 'SUBMITTED'
            OR (
                status = 'IN_PROGRESS'
                AND EXISTS (SELECT 1 FROM property.properties
                            WHERE key = 'pause-resume.prototype.enabled' AND value = 'true')
            )
          )
        AND (SELECT count(lock_key)
            FROM int_lock
            WHERE lock_key = job_uuid
              AND expired_after > (now() AT TIME ZONE 'UTC')) = 0
    ORDER BY created_at;
    """;

    public JobMessageSource(DataSource dataSource) {
        super(dataSource, QUERY_GET_NEXT_UNPROCESSED_JOB);
        setMaxRows(10);
    }


}
