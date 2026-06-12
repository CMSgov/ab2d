package gov.cms.ab2d.coverage.service.v3.audit;

import gov.cms.ab2d.coverage.service.v3.CoverageV3SyncResult;

public interface CoverageV3AuditLog {
	void log(CoverageV3AuditAction action, CoverageV3SyncResult result, String contract, String log, Object data);
}
