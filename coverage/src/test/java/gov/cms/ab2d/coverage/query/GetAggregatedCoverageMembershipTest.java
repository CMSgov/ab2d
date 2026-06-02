package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.coverage.model.Identifiers;
import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import gov.cms.ab2d.coverage.service.v3.CoverageV3ServiceImpl;
import gov.cms.ab2d.coverage.service.v3.CoverageV3SyncService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.ResultSet;
import java.util.*;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;


@Slf4j
@Testcontainers
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class GetAggregatedCoverageMembershipTest {

	@Container
	private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

	CoverageV3SyncService syncService = Mockito.mock(CoverageV3SyncService.class);

	CoverageV3Service coverageV3Service = new CoverageV3ServiceImpl(
			container.getDataSource(),
			Mockito.mock(PropertiesService.class),
			syncService
	);

	@ParameterizedTest
	@ValueSource(strings = {"Z1234", "Z0000", "Z7777"})
	void test(String contract, CapturedOutput output) throws Exception {
		GetAggregatedCoverageMembership aggregatedMembership = new GetAggregatedCoverageMembership(container.getDataSource());
		aggregatedMembership.createAggregatedAttributionTable(contract);

		assertTrue(aggregatedMembership.getDistinctPatientCount(contract) > 0);
		assertTrue(aggregatedMembership.getCoveragePeriodsInAggregatedTable(contract) > 0);

		ContractForCoverageDTO contractDto = new ContractForCoverageDTO();
		contractDto.setContractNumber(contract);

		List<CoverageSummary> coverageSummaries = aggregatedMembership.fetchAggregatedData(contractDto, 1, Optional.empty());
		while (!coverageSummaries.isEmpty()) {
			val lastPatientId = coverageSummaries.get(coverageSummaries.size()-1).getIdentifiers().getPatientIdV3();
			coverageSummaries = aggregatedMembership.fetchAggregatedData(contractDto, 1, Optional.of(lastPatientId));
		}

		aggregatedMembership.createAggregatedAttributionTable(contract);
		// Should delete if exists
		aggregatedMembership.deleteAggregatedTableForContract(contract, Optional.empty());
		// And not fail if it doesn't exist
		aggregatedMembership.deleteAggregatedTableForContract(contract, Optional.empty());
		assertFalse(tableExists("v3.coverage_v3_aggregated_" + contract));

		aggregatedMembership.createAggregatedAttributionTable("Z9999");
		when(syncService.getContractsWithActiveV3Jobs()).thenReturn(List.of("Z9999"));

		aggregatedMembership.createAggregatedAttributionTable(contract);
		assertTrue(tableExists("v3.coverage_v3_aggregated_" + contract));

		coverageV3Service.checkForAggregatedTablesToBeDeleted();
		assertTrue(output.getOut().contains("Deleted table v3.coverage_v3_aggregated_" + contract));
		assertFalse(output.getOut().contains("Deleted table v3.coverage_v3_aggregated_Z9999"));
		assertFalse(tableExists("v3.coverage_v3_aggregated_" + contract));

		aggregatedMembership.createAggregatedAttributionTable(contract);
		aggregatedMembership.deleteAggregatedTableForContract(contract, Optional.of("1234"));
		assertFalse(tableExists("v3.coverage_v3_aggregated_" + contract));

	}

	@Test
	void testDuplicates() {
		val aggregatedMembership = new GetAggregatedCoverageMembership(container.getDataSource());

		var list = List.of(
			new CoverageSummary(Identifiers.ofV3(1L, null, false, 0L), null, List.of()),

			new CoverageSummary(Identifiers.ofV3(2L, null, false, 0L), null, List.of()),
			new CoverageSummary(Identifiers.ofV3(2L, null, false, 0L), null, List.of()),

			new CoverageSummary(Identifiers.ofV3(3L, null, false, 0L), null, List.of()),
			new CoverageSummary(Identifiers.ofV3(4L, null, false, 0L), null, List.of()),

			new CoverageSummary(Identifiers.ofV3(5L, null, false, 0L), null, List.of()),
			new CoverageSummary(Identifiers.ofV3(5L, null, false, 0L), null, List.of()),
			new CoverageSummary(Identifiers.ofV3(5L, null, false, 0L), null, List.of()),

			new CoverageSummary(Identifiers.ofV3(6L, null, false, 0L), null, List.of())
		);

		var indexesOfDuplicatePatients = aggregatedMembership.getIndexesOfDuplicatePatients(list);
		assertEquals("[[1, 2], [5, 6, 7]]", indexesOfDuplicatePatients.toString());


		list = List.of(
			new CoverageSummary(Identifiers.ofV3(2L, null, false, 0L), null, List.of()),
			new CoverageSummary(Identifiers.ofV3(2L, null, false, 0L), null, List.of())
		);

		indexesOfDuplicatePatients = aggregatedMembership.getIndexesOfDuplicatePatients(list);
		assertEquals("[[0, 1]]", indexesOfDuplicatePatients.toString());

	}

	@Test
	void testDuplicatesAndReduce(CapturedOutput output) {

		val aggregatedMembership = new GetAggregatedCoverageMembership(container.getDataSource());

		var list = new ArrayList<>(List.of(
			new CoverageSummary(Identifiers.ofV3(1L, null, false, 1L), null, List.of()),

			new CoverageSummary(Identifiers.ofV3(2L, null, null, 2L), null, List.of()),
			new CoverageSummary(Identifiers.ofV3(2L, null, null, 3L), null, List.of()),

			new CoverageSummary(Identifiers.ofV3(3L, null, null, 4L), null, List.of()),

			new CoverageSummary(Identifiers.ofV3(4L, null, null, 5L), null, List.of()),

			new CoverageSummary(Identifiers.ofV3(5L, null, null, 6L), null, List.of()),
			new CoverageSummary(Identifiers.ofV3(5L, null, null, 7L), null, List.of()),
			new CoverageSummary(Identifiers.ofV3(5L, null, false, 8L), null, List.of()),

			new CoverageSummary(Identifiers.ofV3(6L, null, null, 9L), null, List.of())
		));


		val duplicateIndexes = aggregatedMembership.getIndexesOfDuplicatePatients(list);
		// Rows 2,3 are duplicates as well as 6,7,8
		assertEquals("[[1, 2], [5, 6, 7]]", duplicateIndexes.toString());

		aggregatedMembership.reduce(duplicateIndexes, list);

		assertTrue(output.getOut().contains("Processing duplicate patient at row number 2"));
		assertTrue(output.getOut().contains("Processing duplicate patient at row number 3"));
		assertTrue(output.getOut().contains("Reduced duplicate patient records into record from row number 2"));
		assertTrue(output.getOut().contains("Processing duplicate patient at row number 6"));
		assertTrue(output.getOut().contains("Processing duplicate patient at row number 7"));
		assertTrue(output.getOut().contains("Processing duplicate patient at row number 8"));
		assertTrue(output.getOut().contains("Reduced duplicate patient records into record from row number 6"));
	}

	boolean tableExists(String tableName) throws Exception {
		val query = "SELECT to_regclass('%s') IS NOT NULL".formatted(tableName);
		try (val statement = container.getDataSource().getConnection().prepareStatement(query)) {
			val resultSet = statement.executeQuery();
			resultSet.next();
			return resultSet.getBoolean(1);
		}
	}

}
