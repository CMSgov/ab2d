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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

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

	@Test
	void test(CapturedOutput output) {
		val contract = "Z1234";
//		val contract = "Z0000"; // no columns where share_data == null
//		val contract = "Z7777"; // one MBI with share_data == null
		GetAggregatedCoverageMembership test = new GetAggregatedCoverageMembership(container.getDataSource());
		test.createAggregatedAttributionTable(contract);

		assertTrue(test.getAggregatedTableRowCount(contract) > 0);
		assertTrue(test.getCoveragePeriodsInAggregatedTable(contract) > 0);

		ContractForCoverageDTO contractDto = new ContractForCoverageDTO();
		contractDto.setContractNumber(contract);

		List<CoverageSummary> coverageSummaries = test.fetchAggregatedData(contractDto, 1, Optional.empty());
		while (!coverageSummaries.isEmpty()) {
			System.out.println("coverageSummaries.size() = " + coverageSummaries.size());

			for (CoverageSummary coverageSummary : coverageSummaries) {
				System.out.println("rowNumber=" + coverageSummary.getIdentifiers().getRowNumberV3());
				System.out.println("shareData=" + coverageSummary.getIdentifiers().getShareDataV3());
			}

			val lastPatientId = coverageSummaries.get(coverageSummaries.size()-1).getIdentifiers().getPatientIdV3();
			coverageSummaries = test.fetchAggregatedData(contractDto, 1, Optional.of(lastPatientId));
		}

		// Should delete if exists
		test.deleteAggregatedTable("Z1234");
		// And not fail if it doesn't exist
		test.deleteAggregatedTable("Z1234");


		when(syncService.getContractsWithActiveV3Jobs()).thenReturn(List.of("Z9999"));
		test.createAggregatedAttributionTable("Z9999");
		test.createAggregatedAttributionTable("Z0000");
		test.createAggregatedAttributionTable("Z7777");

		coverageV3Service.checkForAggregatedTablesToBeDeleted();
		assertTrue(output.getOut().contains("Deleted table v3.coverage_v3_aggregated_coverage_v3_aggregated_z0000"));
		assertTrue(output.getOut().contains("Deleted table v3.coverage_v3_aggregated_coverage_v3_aggregated_z7777"));
		assertFalse(output.getOut().contains("Deleted table v3.coverage_v3_aggregated_coverage_v3_aggregated_z9999"));

	}

	@Test
	void testDuplicates() {
		val test = new GetAggregatedCoverageMembership(container.getDataSource());

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


		System.out.println(test.getIndexesOfDuplicatePatients(list));


		list = List.of(
			new CoverageSummary(Identifiers.ofV3(2L, null, false, 0L), null, List.of()),
			new CoverageSummary(Identifiers.ofV3(2L, null, false, 0L), null, List.of())
		);

		System.out.println(test.getIndexesOfDuplicatePatients(list));

	}

	@Test
	void testDuplicatesAndReduce() {

		val test = new GetAggregatedCoverageMembership(container.getDataSource());

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


		val duplicateIndexes = test.getIndexesOfDuplicatePatients(list);

		test.reduce(duplicateIndexes, list);

		val iterator = list.listIterator();
		while (iterator.hasNext()) {
			val next = iterator.next();
			if (next == null) {
				iterator.remove();
			} else {
				val identifiers = next.getIdentifiers();
				if (Objects.equals(Boolean.FALSE, identifiers.getShareDataV3())) {
					log.info("Patient at row number {} has opted out; removing coverage summary", identifiers.getRowNumberV3());
					iterator.remove();
				}
			}
		}


		System.out.println();


	}

}
