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

import static org.junit.Assert.assertTrue;

@Testcontainers
class GetAggregatedCoverageMembershipTest {

	@Container
	private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

	@Test
	void test() {
		//val contract = "Z0000"; // no columns where share_data == null
		val contract = "Z7777"; // one MBI with share_data == null
		GetAggregatedCoverageMembership test = new GetAggregatedCoverageMembership(container.getDataSource());
		test.createAggregatedAttributionTable(contract);

		assertTrue(test.getAggregatedTableRowCount(contract) > 0);
		assertTrue(test.getCoveragePeriodsInAggregatedTable(contract) > 0);

		ContractForCoverageDTO contractDto = new ContractForCoverageDTO();
		contractDto.setContractNumber(contract);

		List<CoverageSummary> coverageSummaries = test.fetchAggregatedData(contractDto, 1, Optional.empty());
		while (!coverageSummaries.isEmpty()) {
			System.out.println("coverageSummaries.size() = " + coverageSummaries.size());

			val lastPatientId = coverageSummaries.get(coverageSummaries.size()-1).getIdentifiers().getPatientIdV3();
			coverageSummaries = test.fetchAggregatedData(contractDto, 1, Optional.of(lastPatientId));
		}
		System.out.println();
	}

}
