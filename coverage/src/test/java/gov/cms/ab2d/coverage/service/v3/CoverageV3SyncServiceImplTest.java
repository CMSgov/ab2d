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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static gov.cms.ab2d.common.util.PropertyConstants.*;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncResult.*;
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

	@Mock
	CoverageV3SyncMetrics metrics;

	@BeforeEach
	void setup() {

		audit = new CoverageV3AuditLogImpl(container.getDataSource(), propertiesService);

		service = new CoverageV3SyncServiceImpl(
			container.getDataSource(),
			lockWrapper,
			lockWrapper,
			audit,
			metrics,
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

		val template = new JdbcTemplate(container.getDataSource());
		template.execute("truncate v3.coverage_v3_audit");
		template.execute("truncate ab2d.bene_coverage_period");
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
	void moveToHistorical_Z0001_bfdSyncInProgress(CapturedOutput out) {
		assertNotEquals(BFD_COVERAGE_SYNC_IN_PROGRESS, service.moveToHistorical("Z0001", CoverageV3SyncSource.CRON_JOB));

		new JdbcTemplate(container.getDataSource()).execute(
		"""
		INSERT INTO ab2d.bene_coverage_period(month, year, status, contract_number)
		VALUES
			(8, 2026, 'IN_PROGRESS', 'Z0001');
		""");

		assertEquals(BFD_COVERAGE_SYNC_IN_PROGRESS, service.moveToHistorical("Z0001", CoverageV3SyncSource.CRON_JOB));

		assertTrue(out.getOut().contains("[V3] Detected BFD coverage sync is in progress for Z0001-2026-8"));
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
	void copyFromStagingTablesToRecent_Z9999_testAuditLogs() {
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
	void copyFromStagingTablesToRecent_Z9999_bfdSyncInProgress(CapturedOutput out) {
		assertNotEquals(BFD_COVERAGE_SYNC_IN_PROGRESS, service.copyFromStagingTablesToRecent("Z9999", CoverageV3SyncSource.CRON_JOB));

		new JdbcTemplate(container.getDataSource()).execute(
		"""
		INSERT INTO ab2d.bene_coverage_period(month, year, status, contract_number)
		VALUES
			(8, 2026, 'IN_PROGRESS', 'Z9999');
		""");

		assertEquals(BFD_COVERAGE_SYNC_IN_PROGRESS, service.copyFromStagingTablesToRecent("Z9999", CoverageV3SyncSource.CRON_JOB));
		assertTrue(out.getOut().contains("[V3] Detected BFD coverage sync is in progress for Z9999-2026-8"));
	}

	@Test
	void copyFromStagingTablesToRecent_TestContract() {
		service = new CoverageV3SyncServiceImpl(
				container.getDataSource(),
				lockWrapper,
				lockWrapper,
				audit,
				metrics,
				propertiesService
		) {
			@Override
			boolean isTestContract(String contract) {
				return true;
			}
		};

		assertEquals(NO_COVERAGE_FOUND_FOR_CONTRACT, service.copyFromStagingTablesToRecent("Z1234", CoverageV3SyncSource.CRON_JOB));
	}

	@Test
	void copyFromStagingTablesToRecent_ContractNotAttested() {
		service = new CoverageV3SyncServiceImpl(
				container.getDataSource(),
				lockWrapper,
				lockWrapper,
				audit,
				metrics,
				propertiesService
		) {
			@Override
			boolean isContractAttested(String contract) {
				return false;
			}
		};

		assertEquals(NO_COVERAGE_FOUND_FOR_CONTRACT, service.copyFromStagingTablesToRecent("Z1234", CoverageV3SyncSource.CRON_JOB));
	}

	@Test
	void copyFromStagingTablesToRecent_ImporterInProgress() {
		when(propertiesService.getProperty(V3_IDR_IMPORTER_STATUS, "")).thenReturn(V3_IDR_IMPORTER_STATUS_IN_PROGRESS);
		assertEquals(IDR_IMPORTER_IN_PROGRESS, service.copyFromStagingTablesToRecent("Z1234", CoverageV3SyncSource.CRON_JOB));
	}

	@Test
	void copyFromStagingTablesToRecent_SourceCronJob() {
		service = new CoverageV3SyncServiceImpl(
				container.getDataSource(),
				lockWrapper,
				lockWrapper,
				audit,
				metrics,
				propertiesService
		) {
			@Override
			boolean contractHasJobInProgress(String contract) {
				return true;
			}

			@Override
			boolean isTestContract(String contract) {
				return false;
			}

			@Override
			boolean isContractAttested(String contract) {
				return true;
			}
		};

		assertEquals(JOB_IN_PROGRESS_FOR_CONTRACT, service.copyFromStagingTablesToRecent("Z1234", CoverageV3SyncSource.CRON_JOB));
	}


	@Test
	void copyFromStagingTablesToRecent_SourceJobHandler() {
		when(lockWrapper.getCoverageLock(any())).thenReturn(lock);
		when(lock.tryLock()).thenReturn(true);
		service = new CoverageV3SyncServiceImpl(
				container.getDataSource(),
				lockWrapper,
				lockWrapper,
				audit,
				metrics,
				propertiesService
		) {
			@Override
			boolean contractHasJobInProgress(String contract) {
				return true;
			}

			@Override
			boolean isTestContract(String contract) {
				return false;
			}

			@Override
			boolean isContractAttested(String contract) {
				return true;
			}
		};

		assertEquals(SYNC_SUCCESSFUL_FOR_CONTRACT, service.copyFromStagingTablesToRecent("Z0000", CoverageV3SyncSource.JOB_HANDLER));
	}

	@Test
	void copyFromStagingTablesToRecent_UnableToAcquireLock(CapturedOutput out) {
		when(lockWrapper.getCoverageLock(any())).thenReturn(lock);
		when(lock.tryLock()).thenReturn(false);
		assertEquals(UNABLE_TO_ACQUIRE_LOCK_FOR_CONTRACT, service.copyFromStagingTablesToRecent("Z0000", CoverageV3SyncSource.JOB_HANDLER));
		assertTrue(out.getOut().contains("[V3] Unable to acquire lock for contract Z0000"));
	}

	@Test
	void testGetContractsInCoverageStagingTable() {
		assertTrue(service.getContractsInCoverageStagingTable().contains("Z1234"));
		assertTrue(service.getContractsInCoverageStagingTable().contains("Z7777"));
	}

	@Test
	void testGetContractsInRecentCoverageTable() {
		assertTrue(service.getContractsInRecentCoverageTable().contains("Z1234"));
		assertTrue(service.getContractsInRecentCoverageTable().contains("Z9999"));
		assertTrue(service.getContractsInRecentCoverageTable().contains("Z7777"));
		assertTrue(service.getContractsInRecentCoverageTable().contains("Z0000"));
	}

	@Test
	void testIsContractAttested() {
		service = new CoverageV3SyncServiceImpl(
				container.getDataSource(),
				lockWrapper,
				lockWrapper,
				audit,
				metrics,
				propertiesService
		);

		assertTrue(service.isContractAttested("ATT1"));
		System.out.println();
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
