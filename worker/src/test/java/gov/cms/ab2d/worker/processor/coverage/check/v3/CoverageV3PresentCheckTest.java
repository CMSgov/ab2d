package gov.cms.ab2d.worker.processor.coverage.check.v3;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import gov.cms.ab2d.coverage.service.v3.CoverageV3ServiceImpl;
import gov.cms.ab2d.coverage.service.v3.CoverageV3SyncService;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@Testcontainers
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CoverageV3PresentCheckTest {

	@Container
	private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

	private CoverageV3CoveragePeriodsPresentCheck check;
	private CoverageV3Service coverageService;

	@BeforeEach
	void setup() {
		coverageService = new CoverageV3ServiceImpl(
			container.getDataSource(),
			Mockito.mock(PropertiesService.class),
			Mockito.mock(CoverageV3SyncService.class)
		);
	}

	@Test
	void testCoveragCheck_Z1234(CapturedOutput out) {
		val issues = new ArrayList<String>();
		val contractNumber = "Z1234";
		val contractDto = new ContractDTO();
		contractDto.setContractType(Contract.ContractType.CLASSIC_TEST);
		contractDto.setContractNumber(contractNumber);
		contractDto.setAttestedOn(OffsetDateTime.parse("2025-09-30T00:00:00+00:00")); // 2025-09-01
		val coverageCounts = coverageService.getCoveragePeriods(List.of(contractDto));

		check = new CoverageV3CoveragePeriodsPresentCheck(coverageService, coverageCounts, issues);
		check.test(contractDto);

		assertTrue(out.getOut().contains("[V3] Z1234-2025-9 no enrollment found"));
		assertTrue(out.getOut().contains("[V3] Z1234-2025-10 no enrollment found"));
		assertTrue(out.getOut().contains("[V3] Z1234-2025-11 no enrollment found"));

		assertFalse(out.getOut().contains("[V3] Z1234-2025-12 no enrollment found"));
		assertFalse(out.getOut().contains("[V3] Z1234-2026-1 no enrollment found"));
		assertFalse(out.getOut().contains("[V3] Z1234-2026-2 no enrollment found"));


	}


}
