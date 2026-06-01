package gov.cms.ab2d.worker.processor.coverage.check.v3;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import gov.cms.ab2d.coverage.service.v3.CoverageV3ServiceImpl;
import gov.cms.ab2d.coverage.service.v3.CoverageV3SyncService;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.ArrayList;


@Testcontainers
class CoverageV3PresentCheckTest {

	@Container
	private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

	private CoverageV3PresentCheck check;
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
	void test() {
		val issues = new ArrayList<String>();
		val coverageCounts = coverageService.getCoverageCount();
		val contractNumber = "Z1234";
		val contractDto = new ContractDTO();
		contractDto.setContractType(Contract.ContractType.CLASSIC_TEST);
		contractDto.setContractNumber(contractNumber);
		contractDto.setAttestedOn(OffsetDateTime.parse("2025-09-30T00:00:00+00:00")); // 2025-09-01

		check = new CoverageV3PresentCheck(coverageService, coverageCounts, issues);
		check.test(contractDto);
	}


}
