package gov.cms.ab2d.worker;

import gov.cms.ab2d.common.model.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.jdbc.JdbcPollingChannelAdapter;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class JobMessageSource extends JdbcPollingChannelAdapter {

    private static final String QUERY_GET_NEXT_UNPROCESSED_JOB = "              " +
                                    "    SELECT *                               " +
                                    "      FROM job                             " +
                                    "     WHERE status = 'SUBMITTED'            " +
                                    "       AND status_message = '0%'           " +
                                    "       AND (SELECT count(lock_key)         " +
                                    "              FROM int_lock                " +
                                    "             WHERE lock_key = job_id) = 0  " +
                                    "  ORDER BY created_at;                     " ;


    public JobMessageSource(DataSource dataSource) {
        super(dataSource, QUERY_GET_NEXT_UNPROCESSED_JOB);
        setMaxRows(1);
        setRowMapper(new JobRowMapper());
    }


    @Slf4j
    public static class JobRowMapper implements RowMapper<Job>
    {
        @Override
        public Job mapRow(ResultSet row, int rowNum) throws SQLException {
            Job job = new Job();
            job.setId(row.getLong("id"));
            job.setJobId(row.getString("job_id"));
//                job.setCreatedAt(OffsetDateTime.parse(row.getString("created_at")));
            job.setCreatedAt(OffsetDateTime.now());

//                final String expires_at = String.valueOf(row.getString("expires_at"));
            job.setExpiresAt(OffsetDateTime.now());

            job.setResourceTypes(row.getString("resource_types"));

//              job.setStatus(row.get("status"));
            job.setStatusMessage(row.getString("status_message"));

            log.info(" ################################################################################");
            log.info(" ROW SEARIALIZED TO Job instance : {} ", job);
            log.info(" ################################################################################");

            return job;
        }
    }

}
