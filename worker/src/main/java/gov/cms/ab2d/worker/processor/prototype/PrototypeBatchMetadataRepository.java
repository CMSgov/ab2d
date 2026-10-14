package gov.cms.ab2d.worker.processor.prototype;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Queries and hard-recovery heal for Spring Batch info that's not readily available elsewhere
 * activeRuntimeSeconds - how long a job has been executing, ignoring time spent paused
 * failedExecutionCount - how many times the job's execution has failed
 * healIndeterminateExecutions - force a job's indeterminate executions/steps to a restartable FAILED state
 *      This is mainly to handle UNKNOWN statuses resulting from zombie workers.
 * completedProcessedCount - how many benes have we processed already? Used to track progress
 * partitionStepNames - every partition the job has run, so recovery can inspect each one
 */
@Slf4j
@Component
public class PrototypeBatchMetadataRepository {

    // If a worker loses the lease, it still can end up performing a write to the Spring Batch metadata
    // which fails and causes the step to have an UNKNOWN status. Spring batch can't guarantee the
    // state of the output so it refuses to restart when there's an UNKNOWN.
    // We heal this to "FAILED" since we just redo the partition anyway.
    private static final String HEAL_STEPS_SQL = """
            UPDATE batch_step_execution se
               SET status = 'FAILED',
                   end_time = COALESCE(se.end_time, now()),
                   version = se.version + 1
              FROM batch_job_execution je
              JOIN batch_job_execution_params p ON p.job_execution_id = je.job_execution_id
             WHERE se.job_execution_id = je.job_execution_id
               AND p.parameter_name = 'jobUuid'
               AND p.parameter_value = :uuid
               AND se.status IN ('STARTING', 'STARTED', 'STOPPING', 'UNKNOWN')
            """;

    // Grabs the winning file for each partition, chosen based on the metadata (not the files)
    // The winning file is always going to be the file with the highest token.
    //
    // step_name is '<workerStep>:partition<index>' so split_part on 'partition' gets the index.
    // each file is uniquely identified by an index + token.
    private static final String COMPLETED_PARTITION_FILES_SQL = """
            SELECT partition_index, fence_token FROM (
                SELECT DISTINCT ON (se.step_name)
                       split_part(se.step_name, '%s', 2)::int AS partition_index,
                       ft.parameter_value::bigint AS fence_token
                  FROM batch_step_execution se
                  JOIN batch_job_execution je ON je.job_execution_id = se.job_execution_id
                  JOIN batch_job_execution_params pj ON pj.job_execution_id = je.job_execution_id
                       AND pj.parameter_name = 'jobUuid'
                  JOIN batch_job_execution_params ft ON ft.job_execution_id = je.job_execution_id
                       AND ft.parameter_name = 'fenceToken'
                 WHERE pj.parameter_value = :uuid
                   AND se.step_name LIKE :stepPrefix
                   AND se.status = 'COMPLETED'
                 ORDER BY se.step_name, ft.parameter_value::bigint DESC
            ) winners
            ORDER BY partition_index
            """.formatted(PrototypePartitionNaming.PARTITION_TOKEN);

    // Gathers the winners from the set of completed partitions to estimate the progress of a job. Used on
    // restart/recovery to seed the value so a restarting job reports approximately accurate progress.
    private static final String COMPLETED_PROCESSED_COUNT_SQL = """
            SELECT COALESCE(SUM(read_count), 0) FROM (
                SELECT DISTINCT ON (se.step_name)
                       se.read_count AS read_count
                  FROM batch_step_execution se
                  JOIN batch_job_execution je ON je.job_execution_id = se.job_execution_id
                  JOIN batch_job_execution_params pj ON pj.job_execution_id = je.job_execution_id
                       AND pj.parameter_name = 'jobUuid'
                  JOIN batch_job_execution_params ft ON ft.job_execution_id = je.job_execution_id
                       AND ft.parameter_name = 'fenceToken'
                 WHERE pj.parameter_value = :uuid
                   AND se.step_name LIKE :stepPrefix
                   AND se.status = 'COMPLETED'
                 ORDER BY se.step_name, ft.parameter_value::bigint DESC
            ) winners
            """;

    // Every partition this job has ever run, whatever generation or status it ended in.
    // Hard recovery uses this to determine which steps to carry forward.
    private static final String PARTITION_STEP_NAMES_SQL = """
            SELECT DISTINCT se.step_name
              FROM batch_step_execution se
              JOIN batch_job_execution je ON je.job_execution_id = se.job_execution_id
              JOIN batch_job_execution_params pj ON pj.job_execution_id = je.job_execution_id
                   AND pj.parameter_name = 'jobUuid'
             WHERE pj.parameter_value = :uuid
               AND se.step_name LIKE :stepPrefix
             ORDER BY se.step_name
            """;

    // When hard-recovering a job, we set its old job execution to FAILED since we're
    // restarting with a new jobExecution anyway.
    private static final String HEAL_JOB_EXECUTIONS_SQL = """
            UPDATE batch_job_execution je
               SET status = 'FAILED',
                   end_time = COALESCE(je.end_time, now()),
                   version = je.version + 1
              FROM batch_job_execution_params p
             WHERE p.job_execution_id = je.job_execution_id
               AND p.parameter_name = 'jobUuid'
               AND p.parameter_value = :uuid
               AND je.status IN ('STARTING', 'STARTED', 'STOPPING', 'UNKNOWN')
            """;

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

    /**
     * Total beneficiaries already processed by partitions completed in prior runs
     */
    public long completedProcessedCount(String jobUuid, String workerStepName) {
        Long count = jdbc.queryForObject(COMPLETED_PROCESSED_COUNT_SQL,
                Map.of("uuid", jobUuid, "stepPrefix",
                        PrototypePartitionNaming.partitionStepNamePattern(workerStepName)), Long.class);
        return count == null ? 0L : count;
    }

    /**
     * Returns the winning output file for every partition of a COMPLETED job
     */
    public List<CompletedPartition> completedPartitionFiles(String jobUuid, String workerStepName) {
        return jdbc.query(COMPLETED_PARTITION_FILES_SQL,
                Map.of("uuid", jobUuid, "stepPrefix",
                        PrototypePartitionNaming.partitionStepNamePattern(workerStepName)),
                (rs, rowNum) -> new CompletedPartition(rs.getInt("partition_index"), rs.getLong("fence_token")));
    }

    /** A partition index paired with the ownership token of the completed generation whose file we keep. */
    public record CompletedPartition(int partitionIndex, long token) {
    }

    /**
     * The step name of every partition this job has run
     */
    public List<String> partitionStepNames(String jobUuid, String workerStepName) {
        return jdbc.queryForList(PARTITION_STEP_NAMES_SQL,
                Map.of("uuid", jobUuid, "stepPrefix",
                        PrototypePartitionNaming.partitionStepNamePattern(workerStepName)), String.class);
    }

    /**
     * On hard recovery, force indeterminate (STARTING, STARTED, STOPPING or UNKNOWN) batch execution/step
     * to FAILED so a resume can happen.
     * Runs after the copy-forward, which allows chunk-level resume so long as there isn't any UNKNOWNs.
     */
    public int healIndeterminateExecutions(String jobUuid) {
        int steps = jdbc.update(HEAL_STEPS_SQL, Map.of("uuid", jobUuid));
        int jobs = jdbc.update(HEAL_JOB_EXECUTIONS_SQL, Map.of("uuid", jobUuid));
        if (steps > 0 || jobs > 0) {
            log.info("hard-recovery heal for job {}: downgraded {} indeterminate step execution(s) and {} job "
                    + "execution(s) to FAILED", jobUuid, steps, jobs);
        }
        return steps;
    }
}
