package gov.cms.ab2d.worker;

import org.springframework.integration.jdbc.JdbcPollingChannelAdapter;

import javax.sql.DataSource;

public class JobMessageSource extends JdbcPollingChannelAdapter {

    private static final String QUERY_GET_NEXT_UNPROCESSED_JOB = "              " +
                                    "    SELECT id                              " +
                                    "      FROM job                             " +
                                    "     WHERE status = 'SUBMITTED'            " +
                                    "       AND status_message = '0%'           " +
                                    "       AND (SELECT count(lock_key)         " +
                                    "              FROM int_lock                " +
                                    "             WHERE lock_key = job_id) = 0  " +
                                    "  ORDER BY created_at;                     ";


    public JobMessageSource(DataSource dataSource) {
        super(dataSource, QUERY_GET_NEXT_UNPROCESSED_JOB);
        setMaxRows(1);
    }


}
