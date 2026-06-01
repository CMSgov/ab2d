package gov.cms.ab2d.coverage.service.v3;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.concurrent.locks.Lock;

@Slf4j
@Component("historicalCoverageLock")
public class CoverageV3HistoricalLockWrapperImpl implements CoverageV3LockWrapper {

    private static final String COVERAGE_V3_LOCK_NAME_PREFIX = "COVERAGE_V3_HISTORICAL_LOCK_";
    private static final int LOCK_TTL_MILLIS = 3_600_000; // 1 hour
    private final JdbcLockRegistry lockRegistry;

    public CoverageV3HistoricalLockWrapperImpl(ApplicationContext context, DataSource dataSource) {
        val defaultLockRepository = new DefaultLockRepository(dataSource);
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
