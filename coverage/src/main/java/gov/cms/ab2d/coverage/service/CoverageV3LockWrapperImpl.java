package gov.cms.ab2d.coverage.service;

import lombok.val;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.concurrent.locks.Lock;

@Component
public class CoverageV3LockWrapperImpl implements CoverageV3LockWrapper {

    private static final String COVERAGE_V3_LOCK_NAME_PREFIX = "COVERAGE_V3_LOCK_";
    private static final int LOCK_TTL_MILLIS = 120_000;
    private final JdbcLockRegistry lockRegistry;

    public CoverageV3LockWrapperImpl(ApplicationContext context, DataSource dataSource) {
        val defaultLockRepository = new DefaultLockRepository(dataSource);

        // A lock is automatically killed, regardless of whether it is in use
        // if it has not been renewed TTL seconds after it was created.
        // What this means is that if you are locking longer than this TTL, then
        // you need to renew the lock otherwise you will lose it and get undefined
        // behavior when you attempt to unlock your lock.
        defaultLockRepository.setApplicationContext(context);
        defaultLockRepository.setTimeToLive(LOCK_TTL_MILLIS);
        defaultLockRepository.afterSingletonsInstantiated();
        defaultLockRepository.afterPropertiesSet();
        this.lockRegistry = new JdbcLockRegistry(defaultLockRepository);
    }

    @Override
    public Lock getCoverageLock(String contract) {
        val lockName = COVERAGE_V3_LOCK_NAME_PREFIX + contract;
        return lockRegistry.obtain(lockName);
    }
}
