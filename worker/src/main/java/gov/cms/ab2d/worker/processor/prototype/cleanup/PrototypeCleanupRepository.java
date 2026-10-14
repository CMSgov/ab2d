package gov.cms.ab2d.worker.processor.prototype.cleanup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * SQL queries for cleaning up spring batch metadata. Essentially deletes anything older than
 * 30 days.
 */
@Slf4j
@Repository
public class PrototypeCleanupRepository {

    // Find executions old enough to delete that are also in a finished state
    // technically a job can be paused indefinitely, so checking the status is done out of
    // an abundance of caution. It's limited to :maxExecutions to avoid doing huge deletions all at once.
    private static final String ELIGIBLE_EXECUTIONS_SQL = """
            SELECT je.job_execution_id, p.parameter_value AS job_uuid
              FROM batch_job_execution je
              LEFT JOIN batch_job_execution_params p
                     ON p.job_execution_id = je.job_execution_id
                    AND p.parameter_name = 'jobUuid'
              LEFT JOIN job j ON j.job_uuid = p.parameter_value
             WHERE COALESCE(je.end_time, je.create_time) < now() - make_interval(days => :retentionDays)
               AND (p.parameter_value IS NULL
                    OR j.status IS NULL
                    OR j.status IN ('SUCCESSFUL', 'FAILED', 'CANCELLED'))
             ORDER BY je.job_execution_id
             LIMIT :maxExecutions
            """;

    // All the deletion SQL takes the executions gathered in the step above and
    // delete all associated data.
    private static final String DELETE_STEP_EXECUTION_CONTEXT_SQL = """
            DELETE FROM batch_step_execution_context
             WHERE step_execution_id IN (SELECT step_execution_id
                                           FROM batch_step_execution
                                          WHERE job_execution_id IN (:ids))
            """;

    private static final String DELETE_STEP_EXECUTION_SQL = """
            DELETE FROM batch_step_execution WHERE job_execution_id IN (:ids)
            """;

    private static final String DELETE_JOB_EXECUTION_CONTEXT_SQL = """
            DELETE FROM batch_job_execution_context WHERE job_execution_id IN (:ids)
            """;

    private static final String DELETE_JOB_EXECUTION_PARAMS_SQL = """
            DELETE FROM batch_job_execution_params WHERE job_execution_id IN (:ids)
            """;

    private static final String DELETE_JOB_EXECUTION_SQL = """
            DELETE FROM batch_job_execution WHERE job_execution_id IN (:ids)
            """;

    private static final String DELETE_LEASES_SQL = """
            DELETE FROM ab2d.job_lease WHERE job_uuid IN (:uuids)
            """;

    // after we delete all its metadata, the job instance is safe to delete
    private static final String DELETE_ORPHAN_JOB_INSTANCES_SQL = """
            DELETE FROM batch_job_instance bi
             WHERE NOT EXISTS (SELECT 1 FROM batch_job_execution je
                                WHERE je.job_instance_id = bi.job_instance_id)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public PrototypeCleanupRepository(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    public List<EligibleExecution> eligibleExecutions(int retentionDays, int maxExecutions) {
        return jdbc.query(ELIGIBLE_EXECUTIONS_SQL,
                Map.of("retentionDays", retentionDays, "maxExecutions", maxExecutions),
                (rs, rowNum) -> new EligibleExecution(rs.getLong("job_execution_id"), rs.getString("job_uuid")));
    }

    /**
     * Associates a batch execution with a job ID, which keeps Spring Batch's metadata in line with AB2D's job db
     */
    public record EligibleExecution(long executionId, String jobUuid) {
    }

    /**
     * Delete the given executions and everything associated with them
     *
     * @return total rows removed across the five tables
     */
    public long deleteExecutions(List<Long> executionIds) {
        if (executionIds.isEmpty()) {
            return 0L;
        }
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("ids", executionIds);
        long deleted = 0;

        // Children must be deleted before parents to avoid foreign key errors
        deleted += jdbc.update(DELETE_STEP_EXECUTION_CONTEXT_SQL, params);
        deleted += jdbc.update(DELETE_STEP_EXECUTION_SQL, params);
        deleted += jdbc.update(DELETE_JOB_EXECUTION_CONTEXT_SQL, params);
        deleted += jdbc.update(DELETE_JOB_EXECUTION_PARAMS_SQL, params);
        deleted += jdbc.update(DELETE_JOB_EXECUTION_SQL, params);
        return deleted;
    }

    /**
     * Helper function for deleting ownership rows
     */
    public int deleteLeases(List<String> jobUuids) {
        if (jobUuids.isEmpty()) {
            return 0;
        }
        return jdbc.update(DELETE_LEASES_SQL, Map.of("uuids", jobUuids));
    }

    /**
     * Drop job instances that we just orphaned (have no execution metadata anymore)
     */
    public int deleteOrphanJobInstances() {
        return jdbc.update(DELETE_ORPHAN_JOB_INSTANCES_SQL, Map.of());
    }
}
