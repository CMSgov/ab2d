package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

@Testcontainers
class GetAggregatedCoverageMembershipTest {

	@Container
	private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

	@Test
	void test() {
		val contract = "Z0000";
		GetAggregatedCoverageMembership test = new GetAggregatedCoverageMembership(container.getDataSource());
		test.createAggregatedAttributionTable(contract);
		System.out.println(test.getAggregatedTableRowCount(contract));

		ContractForCoverageDTO contractDto = new ContractForCoverageDTO();
		contractDto.setContractNumber(contract);


		List<CoverageSummary> coverageSummaries = test.fetchAggregatedData(contractDto, 100, Optional.empty());
		for (CoverageSummary coverageSummary : coverageSummaries) {
			System.out.println();
		}
		System.out.println();
	}

}
