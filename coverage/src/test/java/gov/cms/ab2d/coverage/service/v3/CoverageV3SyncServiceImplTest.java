package gov.cms.ab2d.coverage.service.v3;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.util.DatadogSpans;
import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import gov.cms.ab2d.coverage.service.v3.audit.CoverageV3AuditLog;
import gov.cms.ab2d.coverage.service.v3.audit.CoverageV3AuditLogImpl;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static gov.cms.ab2d.common.util.PropertyConstants.V3_AUDIT_LOGGING_ENABLED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CoverageV3SyncServiceImplTest {

	@Container
	private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

	CoverageV3SyncServiceImpl service;
	CoverageV3AuditLog audit;

	@Mock
	PropertiesService propertiesService;

	@Mock
	CoverageV3LockWrapper lockWrapper;

	@Mock
	Lock lock;

	@BeforeEach
	void setup() {

		audit = new CoverageV3AuditLogImpl(container.getDataSource(), propertiesService);

		service = new CoverageV3SyncServiceImpl(
			container.getDataSource(),
			lockWrapper,
			lockWrapper,
			audit,
			propertiesService
		) {
			@Override
			boolean isTestContract(String contract) {
				return false;
			}

			@Override
			boolean isContractAttested(String contract) {
				return true;
			}
		};

		when(propertiesService.isToggleOn(V3_AUDIT_LOGGING_ENABLED, false)).thenReturn(true);

		new JdbcTemplate(container.getDataSource()).execute("truncate v3.coverage_v3_audit");
	}

	@Test
	void moveToHistorical_Z0001_testAuditLogs() {
		when(propertiesService.isToggleOn(V3_AUDIT_LOGGING_ENABLED, false)).thenReturn(true);
		service.moveToHistorical("Z0001", CoverageV3SyncSource.CRON_JOB);

		val rows = getAuditLogs();

		assertAuditLogEquals(rows.get(0),
		"""
		{action=COPY_TO_HISTORICAL, result=JOB_IN_PROGRESS_FOR_CONTRACT, contract=Z0001, log=, data={}}
		""");
	}

	@Test
	void moveToHistorical_Z1234_testAuditLogs() {
		when(propertiesService.isToggleOn(V3_AUDIT_LOGGING_ENABLED, false)).thenReturn(true);
		when(lockWrapper.getCoverageLock(any())).thenReturn(lock);
		when(lock.tryLock()).thenReturn(true);
		service.moveToHistorical("Z1234", CoverageV3SyncSource.CRON_JOB);

		val rows = getAuditLogs();

		assertAuditLogEquals(rows.get(0),
		"""
		{action=COPY_TO_HISTORICAL, result=, contract=Z1234, log=, data={"rowsMoved": 3}}
		""");

		assertAuditLogEquals(rows.get(1),
		"""
		{action=COPY_TO_HISTORICAL, result=, contract=Z1234, log=Populated history summary table, data={}}
		""");

		assertAuditLogEquals(rows.get(2),
		"""
		{action=COPY_TO_HISTORICAL, result=, contract=Z1234, log=Populated history summary coverage period table, data={}}
		""");

	}

	@Test
	void moveToStaging_Z9999_testAuditLogs() {
		when(propertiesService.isToggleOn(V3_AUDIT_LOGGING_ENABLED, false)).thenReturn(true);
		when(lockWrapper.getCoverageLock(any())).thenReturn(lock);
		when(lock.tryLock()).thenReturn(true);
		service.copyFromStagingTablesToRecent("Z9999", CoverageV3SyncSource.CRON_JOB);

		val rows = new JdbcTemplate(container.getDataSource()).queryForList("select * from v3.coverage_v3_audit");

		assertAuditLogEquals(rows.get(6),
		"""
		{action=COPY_FROM_STAGING, result=SYNC_SUCCESSFUL_FOR_CONTRACT, contract=Z9999, log=, data={"rowsInStagingDeleted": 4, "rowsInCoverageAfterCopy": 4}}
		""");

	}

	@Test
	void moveToHistorical_Z1234_tagsSpanAndRecordsMetrics() {
		when(propertiesService.isToggleOn(V3_AUDIT_LOGGING_ENABLED, false)).thenReturn(true);
		when(lockWrapper.getCoverageLock(any())).thenReturn(lock);
		when(lock.tryLock()).thenReturn(true);

		try (MockedStatic<DatadogSpans> spans = mockStatic(DatadogSpans.class)) {
			service.moveToHistorical("Z1234", CoverageV3SyncSource.CRON_JOB);

			spans.verify(() -> DatadogSpans.setTag("contract", "Z1234"));
			spans.verify(() -> DatadogSpans.setTag("component", "coverage"));
			spans.verify(() -> DatadogSpans.setTag("sync.source", "CRON_JOB"));
			spans.verify(() -> DatadogSpans.setMetric("coverage.v3.rows_moved", 3L));
		}
	}

	void assertAuditLogEquals(Map<String, Object> result, String string) {
		// remove id and timestamp to simplify comparisons
		result.remove("timestamp");
		result.remove("id");
		string = string.trim();
		assertEquals(result.toString().trim(), string.trim());
	}

	List<Map<String, Object>>  getAuditLogs() {
		return new JdbcTemplate(container.getDataSource()).queryForList("select * from v3.coverage_v3_audit");
	}

}
