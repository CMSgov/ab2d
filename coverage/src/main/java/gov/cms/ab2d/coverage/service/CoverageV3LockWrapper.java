package gov.cms.ab2d.coverage.service;

import lombok.val;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.concurrent.locks.Lock;

@Component
/**
 * Used to lock the database so that only one application or thread can retrieve a value from
 * the database at a time since it also deletes the row. A thread can retrieve its own lock.
 *
 * This is based on Spring Integration and relies on the int_lock table. This and related tables
 * should be created by Spring Integration on worker startup, although not necessarily before tests
 * are run so if you don't have these tables, run worker first. The lock works by putting an
 * entry in the int_lock table (that expires after an amount of time). When the lock is released,
 * the entry is deleted. This allows different instances of worker to not step on each other.
 */
public class CoverageV3LockWrapper {

    private static final String COVERAGE_V3_LOCK_NAME_PREFIX = "COVERAGE_V3_LOCK_";
    private static final int LOCK_REPO_TTL_MILLIS = 120_000;
    private final JdbcLockRegistry lockRegistry;

    public CoverageV3LockWrapper(ApplicationContext context, DataSource dataSource) {
        val defaultLockRepository = new DefaultLockRepository(dataSource);

        // A lock is automatically killed, regardless of whether it is in use
        // if it has not been renewed TTL seconds after it was created.
        // What this means is that if you are locking longer than this TTL, then
        // you need to renew the lock otherwise you will lose it and get undefined
        // behavior when you attempt to unlock your lock.
        defaultLockRepository.setApplicationContext(context);
        defaultLockRepository.setTimeToLive(LOCK_REPO_TTL_MILLIS);
        defaultLockRepository.afterSingletonsInstantiated();
        defaultLockRepository.afterPropertiesSet();
        this.lockRegistry = new JdbcLockRegistry(defaultLockRepository);
    }

    public Lock getCoverageLock(String contract) {
        val lockName = COVERAGE_V3_LOCK_NAME_PREFIX + contract;
        return lockRegistry.obtain(lockName);
    }
}
