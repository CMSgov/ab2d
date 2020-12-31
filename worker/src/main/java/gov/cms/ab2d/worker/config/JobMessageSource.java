package gov.cms.ab2d.worker.config;

import org.springframework.integration.jdbc.JdbcPollingChannelAdapter;

import javax.sql.DataSource;

/**
 * Extends the JdbcPollingChannelAdapter to implement a Job table specific polling Adapter.
 * The SQL query is specified as a static constant.
 */
public class JobMessageSource extends JdbcPollingChannelAdapter {

    private static final String QUERY_GET_NEXT_UNPROCESSED_JOB = "                  " +
                                    "    SELECT id, job_uuid, status                " +
                                    "      FROM job                                 " +
                                    "     WHERE status = 'SUBMITTED'                " +
                                    "       AND (SELECT count(lock_key)             " +
                                    "              FROM int_lock                    " +
                                    "             WHERE lock_key = job_uuid) = 0    " +
                                    "  ORDER BY created_at;                         ";


    public JobMessageSource(DataSource dataSource) {
        super(dataSource, QUERY_GET_NEXT_UNPROCESSED_JOB);
        setMaxRows(10);
    }


}
