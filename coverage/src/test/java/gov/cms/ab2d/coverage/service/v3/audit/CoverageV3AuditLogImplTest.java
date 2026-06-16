package gov.cms.ab2d.coverage.service.v3.audit;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.util.PropertyConstants;
import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import gov.cms.ab2d.coverage.service.v3.CoverageV3SyncResult;
import gov.cms.ab2d.coverage.service.v3.CoverageV3SyncServiceImpl;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static gov.cms.ab2d.common.util.PropertyConstants.*;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncResult.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CoverageV3AuditLogImplTest {

	@Container
	private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

	@Mock
	PropertiesService propertiesService;

	CoverageV3AuditLogImpl audit;

	@BeforeEach
	void setup() {
		when(propertiesService.isToggleOn(V3_AUDIT_LOGGING_ENABLED, false)).thenReturn(true);
		audit = new CoverageV3AuditLogImpl(container.getDataSource(), propertiesService);
	}

	@Test
	void testAuditInputFields() {
		audit.log(CoverageV3AuditAction.COPY_FROM_STAGING, null, "Z0001", null, null);
		audit.log(CoverageV3AuditAction.COPY_FROM_STAGING, JOB_IN_PROGRESS_FOR_CONTRACT, "Z0002", null, null);
		audit.log(CoverageV3AuditAction.COPY_FROM_STAGING, IDR_IMPORTER_IN_PROGRESS, "Z0003", "IDR importer in progress", null);
		audit.log(CoverageV3AuditAction.COPY_FROM_STAGING, SYNC_SUCCESSFUL_FOR_CONTRACT, "Z0004", null, Map.of("rowsMoved", 999));

		val rows = new JdbcTemplate(container.getDataSource()).queryForList("select * from v3.coverage_v3_audit");
		assertEquals(4, rows.size());

		assertEquals("COPY_FROM_STAGING", rows.get(0).get("action"));
		assertEquals("", rows.get(0).get("result"));
		assertEquals("Z0001", rows.get(0).get("contract"));
		assertEquals("", rows.get(0).get("log"));
		assertEquals("{}", rows.get(0).get("data").toString());

		assertEquals("IDR importer in progress", rows.get(2).get("log"));

		assertEquals("COPY_FROM_STAGING", rows.get(3).get("action"));
		assertEquals("Z0004", rows.get(3).get("contract"));
		assertEquals("", rows.get(3).get("log"));
		assertEquals("{\"rowsMoved\": 999}", rows.get(3).get("data").toString());

	}

}
