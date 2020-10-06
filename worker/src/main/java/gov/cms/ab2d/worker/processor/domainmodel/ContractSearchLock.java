package gov.cms.ab2d.worker.processor.domainmodel;

import gov.cms.ab2d.common.model.CoverageSearch;
import gov.cms.ab2d.common.repository.CoverageSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Optional;
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
public class ContractSearchLock {
    @Autowired
    private DataSource dataSource;
    @Autowired
    private CoverageSearchRepository coverageSearchRepository;
    private JdbcLockRegistry lockRegistry;
    private static final String SEARCH_LOCK_NAME = "SEARCH_LOCK";
    private static final int LOCK_TIME = 60_000; // 60 seconds

    public JdbcLockRegistry contractLockRegistry(LockRepository lockRepository) {
        return new JdbcLockRegistry(lockRepository);
    }

    public LockRepository contractLockRepository() {
        final DefaultLockRepository defaultLockRepository = new DefaultLockRepository(dataSource);
        defaultLockRepository.setTimeToLive(LOCK_TIME);
        defaultLockRepository.afterPropertiesSet();
        return defaultLockRepository;
    }

    public Lock getLock(String lock_id) {
        if (lockRegistry == null) {
            lockRegistry = contractLockRegistry(contractLockRepository());
        }
        Lock lock = lockRegistry.obtain(lock_id);
        return lock;
    }

    /**
     * This is the most important part of the class. It retrieves the next search in the table
     * assuming that another thread or application is not currently pulling anything from the table.
     * If there are no jobs to pull or the table is locked, it returns null
     *
     * @return the next search or else null if there are none or if the table is locked
     */
    public CoverageSearch getNextSearch() {
        Lock lock = getLock(SEARCH_LOCK_NAME);
        if (lock.tryLock()) {
            try {
                // manipulate protected state
                Optional<CoverageSearch> searchOpt = coverageSearchRepository.findFirstByOrderByCreatedAsc();
                if (searchOpt.isEmpty()) {
                    return null;
                }
                CoverageSearch search = searchOpt.get();
                coverageSearchRepository.delete(search);
                search.setId(null);
                return search;
            } finally {
                lock.unlock();
            }
        } else {
            // perform alternative actions
            return null;
        }
    }
}
