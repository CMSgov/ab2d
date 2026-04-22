package gov.cms.ab2d.coverage.service.v3;

import java.util.concurrent.locks.Lock;

public interface CoverageV3LockWrapper {
	Lock getCoverageLock(String contract);
}
