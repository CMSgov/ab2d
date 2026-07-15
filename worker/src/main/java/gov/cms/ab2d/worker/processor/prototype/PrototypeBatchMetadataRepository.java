package gov.cms.ab2d.worker.processor.prototype;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Read-only queries for Spring Batch info that's not readily available elsewhere
 * activeRuntimeSeconds - how long a job has been executing, ignoring time spent paused
 * failedExecutionCount - how many times the job's execution has failed
 */
@Slf4j
@Component
public class PrototypeBatchMetadataRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PrototypeBatchMetadataRepository(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * active job runtime, calculated with the sum of the duration of job executions
     */
    public long activeRuntimeSeconds(String jobUuid) {
        val sql = """
                SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (COALESCE(je.end_time, now()) - je.start_time))), 0)
                FROM batch_job_execution je
                JOIN batch_job_execution_params p ON p.job_execution_id = je.job_execution_id
                WHERE p.parameter_name = 'jobUuid'
                  AND p.parameter_value = :uuid
                  AND je.start_time IS NOT NULL
                """;
        Double seconds = jdbc.queryForObject(sql, Map.of("uuid", jobUuid), Double.class);
        return seconds == null ? 0L : seconds.longValue();
    }

    public int failedExecutionCount(String jobUuid) {
        val sql = """
                SELECT COUNT(*)
                FROM batch_job_execution je
                JOIN batch_job_execution_params p ON p.job_execution_id = je.job_execution_id
                WHERE p.parameter_name = 'jobUuid'
                  AND p.parameter_value = :uuid
                  AND je.status = 'FAILED'
                """;
        Integer count = jdbc.queryForObject(sql, Map.of("uuid", jobUuid), Integer.class);
        return count == null ? 0 : count;
    }
}
