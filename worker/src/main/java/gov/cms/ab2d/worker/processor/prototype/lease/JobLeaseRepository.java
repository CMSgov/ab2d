package gov.cms.ab2d.worker.processor.prototype.lease;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ownership store for hard-crash recovery.
 * Contains the following information:
 *      token: identifies who currently own the job
 *      heartbeat_at: signals that the worker is alive
 *      clean_suspend_token: allows workers to cleanly restart from a graceful shutdown
 *
 * The fence token enforces that only one worker can commit chunks for a job at a time
 * The suspend_token just indicates that the worker last working on this job gracefully shut down, it
 * doesn't enforce anything. The graceful shutdown is correct by construction as setting this token is the last
 * action the shutdown routine does.
 *
 * All transactional operations carry a fence token so that expired workers cannot make any further changes.
 */
@Slf4j
@Repository
public class JobLeaseRepository {

    // Create a new token, or bump the current token +1. Clears the suspend token since we can't be suspended
    // if we're bumping the token.
    private static final String BUMP_SQL = """
            INSERT INTO ab2d.job_lease (job_uuid, owner, token, heartbeat_at, clean_suspend_token)
            VALUES (:jobUuid, :owner, 1, now(), NULL)
            ON CONFLICT (job_uuid) DO UPDATE
                SET token = job_lease.token + 1,
                    owner = :owner,
                    heartbeat_at = now(),
                    clean_suspend_token = NULL
            RETURNING token
            """;

    // Soft resume, returns the token so that the new worker picks up directly
    // where the previous one left off and uses the same output file. This is the "true pause" scenario.
    private static final String ADOPT_CLEAN_SUSPEND_SQL = """
            UPDATE ab2d.job_lease
               SET owner = :owner,
                   heartbeat_at = now(),
                   clean_suspend_token = NULL
             WHERE job_uuid = :jobUuid
               AND clean_suspend_token IS NOT NULL
               AND clean_suspend_token = token
            RETURNING token
            """;

    // Beat the worker's heart, but only if we hold the current token
    private static final String ASSERT_AND_BEAT_SQL = """
            UPDATE ab2d.job_lease SET heartbeat_at = now()
             WHERE job_uuid = :jobUuid AND token = :token
            """;

    // If we hold the current token, mark this job as cleanly suspended
    private static final String MARK_CLEAN_SUSPEND_SQL = """
            UPDATE ab2d.job_lease SET clean_suspend_token = :token
             WHERE job_uuid = :jobUuid AND token = :token
            """;

    private static final String CURRENT_TOKEN_SQL = """
            SELECT token FROM ab2d.job_lease WHERE job_uuid = :jobUuid
            """;

    // Find a job
    private static final String FIND_SQL = """
            SELECT owner, token, clean_suspend_token,
                   (heartbeat_at < now() - make_interval(secs => :ttlSeconds)) AS stale
              FROM ab2d.job_lease
             WHERE job_uuid = :jobUuid
            """;

    // Fleet-wide heartbeat health across every IN_PROGRESS job. Stale means past the TTL and therefore
    // eligible for takeover, which is normal and self-healing. Unrecovered means past the longer grace
    // window, so a takeover should already have happened and did not.
    private static final String HEARTBEAT_HEALTH_SQL = """
            SELECT
                count(*) FILTER (WHERE l.heartbeat_at >= now() - make_interval(secs => :ttlSeconds)) AS active,
                count(*) FILTER (WHERE l.heartbeat_at < now() - make_interval(secs => :ttlSeconds)) AS stale,
                count(*) FILTER (WHERE l.heartbeat_at < now() - make_interval(secs => :graceSeconds))
                    AS unrecovered
              FROM ab2d.job_lease l
              JOIN job j ON j.job_uuid = l.job_uuid
             WHERE j.status = 'IN_PROGRESS'
            """;

    // The jobs behind an unrecovered count, so the alert can name them instead of just counting them.
    private static final String UNRECOVERED_JOBS_SQL = """
            SELECT l.job_uuid
              FROM ab2d.job_lease l
              JOIN job j ON j.job_uuid = l.job_uuid
             WHERE j.status = 'IN_PROGRESS'
               AND l.heartbeat_at < now() - make_interval(secs => :graceSeconds)
             ORDER BY l.heartbeat_at
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public JobLeaseRepository(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * Bump the token or create a new one
     */
    public long bump(String jobUuid, String owner) {
        Long token = jdbc.queryForObject(BUMP_SQL, Map.of("jobUuid", jobUuid, "owner", owner), Long.class);
        if (token == null) {
            throw new IllegalStateException("job_lease bump returned no token for " + jobUuid);
        }
        log.debug("bumped lease for job {} to token {} (owner {})", jobUuid, token, owner);
        return token;
    }

    /**
     * Try to soft resume, only possible if the suspend_token is present in the lease row
     */
    public Optional<Long> tryAdoptCleanSuspend(String jobUuid, String owner) {
        try {
            Long token = jdbc.queryForObject(ADOPT_CLEAN_SUSPEND_SQL,
                    Map.of("jobUuid", jobUuid, "owner", owner), Long.class);
            if (token != null) {
                log.info("adopted clean-suspend lease for job {} at token {} (owner {}) - soft resume", jobUuid, token, owner);
            }
            return Optional.ofNullable(token);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Called in every chunk transaction to ensure that:
     *      1) We don't commit any chunks if we lost the token
     *      2) Our heartbeat liveness is up-to-date
     */
    public void assertHoldsAndBeat(String jobUuid, long token) {
        int rows = jdbc.update(ASSERT_AND_BEAT_SQL, Map.of("jobUuid", jobUuid, "token", token));
        if (rows == 0) {
            throw new FenceLostException(jobUuid, token);
        }
    }

    /**
     * Same as above, but reports with a boolean instead of throwing. The scheduled renewer needs to keep
     * looping over other jobs rather than fail on an exception so it uses this variant.
     */
    public boolean renewHeartbeat(String jobUuid, long token) {
        int rows = jdbc.update(ASSERT_AND_BEAT_SQL, Map.of("jobUuid", jobUuid, "token", token));
        log.debug("heartbeat renew for job {} at token {} affected {} row(s)", jobUuid, token, rows);
        return rows == 1;
    }

    /**
     * Record a clean shutdown for a job
     */
    public boolean markCleanSuspend(String jobUuid, long token) {
        boolean marked = jdbc.update(MARK_CLEAN_SUSPEND_SQL, Map.of("jobUuid", jobUuid, "token", token)) == 1;
        if (marked) {
            log.info("recorded clean suspend for job {} at token {}", jobUuid, token);
        } else {
            log.warn("could not record clean suspend for job {} at token {} - superseded during shutdown", jobUuid, token);
        }
        return marked;
    }

    /**
     * Current token for a job
     */
    public Optional<Long> currentToken(String jobUuid) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(CURRENT_TOKEN_SQL, Map.of("jobUuid", jobUuid), Long.class));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /** Figure out if a job exists and whether we need to soft or hard-resume it */
    public Optional<Lease> find(String jobUuid, int ttlSeconds) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("jobUuid", jobUuid)
                .addValue("ttlSeconds", ttlSeconds);
        try {
            return Optional.ofNullable(jdbc.queryForObject(FIND_SQL, params, (rs, rowNum) -> {
                long token = rs.getLong("token");
                long cleanSuspend = rs.getLong("clean_suspend_token");
                Long cleanSuspendToken = rs.wasNull() ? null : cleanSuspend;
                return new Lease(rs.getString("owner"), token, cleanSuspendToken, rs.getBoolean("stale"));
            }));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Count the IN_PROGRESS jobs whose lease heartbeat is healthy, stale, or so old that recovery should
     * already have taken over.
     *
     * @param ttlSeconds   heartbeat age past which a job is eligible for takeover
     * @param graceSeconds heartbeat age past which a takeover should already have happened
     */
    public HeartbeatHealth heartbeatHealth(int ttlSeconds, int graceSeconds) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ttlSeconds", ttlSeconds)
                .addValue("graceSeconds", graceSeconds);
        HeartbeatHealth health = jdbc.queryForObject(HEARTBEAT_HEALTH_SQL, params,
                (rs, rowNum) -> new HeartbeatHealth(rs.getLong("active"), rs.getLong("stale"),
                        rs.getLong("unrecovered")));
        return health == null ? new HeartbeatHealth(0, 0, 0) : health;
    }

    /** The job uuids whose heartbeat is older than the grace window. */
    public List<String> unrecoveredJobUuids(int graceSeconds) {
        return jdbc.queryForList(UNRECOVERED_JOBS_SQL,
                new MapSqlParameterSource("graceSeconds", graceSeconds), String.class);
    }

    /** Current lease state for a job. */
    public record Lease(String owner, long token, Long cleanSuspendToken, boolean heartbeatStale) {
    }

    /** How many IN_PROGRESS jobs have a healthy, stale, or long-past-stale lease heartbeat. */
    public record HeartbeatHealth(long active, long stale, long unrecovered) {
    }
}
