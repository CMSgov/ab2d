package gov.cms.ab2d.worker.processor.coverage.check;

import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoverageCount;
import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearchEvent;
import gov.cms.ab2d.coverage.service.CoverageService;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CoverageCheckPredicatesUnitTest {

    @Mock
    private CoverageService coverageService;

    private static final ZonedDateTime CURRENT_TIME = OffsetDateTime.now().atZoneSameInstant(AB2D_ZONE);
    private static ZonedDateTime ATTESTATION_TIME = CURRENT_TIME.minusMonths(3);

    @AfterEach
    void tearDown() {
        reset(coverageService);
    }

    @DisplayName("Coverage periods outright missing for contract is detected")
    @Test
    void whenCoveragePeriodsMissing_failPresentCheck() {

        when(coverageService.getCoveragePeriod(any(), anyInt(), anyInt())).thenThrow(EntityNotFoundException.class);

        List<String> issues = new ArrayList<>();
        CoveragePeriodsPresentCheck presentCheck = new CoveragePeriodsPresentCheck(coverageService, null, issues);

        ContractDTO contract = getContractDTO();

        assertFalse(presentCheck.test(contract));

        assertEquals(4, issues.size());
        issues.forEach(issue -> assertTrue(issue.contains("coverage period missing")));
    }


    private ContractDTO getContractDTO() {
        return new ContractDTO("TEST", null, ATTESTATION_TIME.toOffsetDateTime(), Contract.ContractType.NORMAL);
    }

    @DisplayName("Coverage periods all present for contract check passes")
    @Test
    void whenCoveragePeriodsPresent_passPresentCheck() {

        ContractDTO contract = getContractDTO();

        // Just reuse, check assumes getCoveragePeriod works
        CoveragePeriod coveragePeriod = new CoveragePeriod();
        coveragePeriod.setContractNumber(contract.getContractNumber());
        coveragePeriod.setYear(ATTESTATION_TIME.getYear());
        coveragePeriod.setMonth(ATTESTATION_TIME.getMonthValue());

        doReturn(coveragePeriod).when(coverageService)
                .getCoveragePeriod(any(ContractForCoverageDTO.class), anyInt(), anyInt());

        List<String> issues = new ArrayList<>();
        CoveragePeriodsPresentCheck presentCheck = new CoveragePeriodsPresentCheck(coverageService, null, issues);

        assertTrue(presentCheck.test(contract));

        assertTrue(issues.isEmpty());
    }

    @DisplayName("Any coverage periods missing coverage for contract then fails check")
    @Test
    void whenCoveragePeriodsNoCoveragePresent_failCoveragePresentCheck() {

        Map<String, List<CoverageCount>> coverageCounts = new HashMap<>();
        List<String> issues = new ArrayList<>();
        CoveragePresentCheck presentCheck =
                new CoveragePresentCheck(coverageService, coverageCounts, issues);

        ContractDTO contract = getContractDTO();

        // Fail when no coverage is present
        assertFalse(presentCheck.test(contract));
        assertEquals(1, issues.size());
        assertEquals("TEST has no enrollment", issues.get(0));


        // Fail if recent coverage period missing
        issues.clear();
        List<CoverageCount> fakeCounts = List.of(
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(0).getYear(),
                        ATTESTATION_TIME.plusMonths(0).getMonthValue(), 1, 1, 1),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(1).getYear(),
                        ATTESTATION_TIME.plusMonths(1).getMonthValue(), 1, 1, 1)
        );
        coverageCounts.put("TEST", fakeCounts);

        assertFalse(presentCheck.test(contract));

        assertEquals(1, issues.size());
        issues.forEach(issue -> assertTrue(issue.contains("no enrollment found")));

        // Fail if intermediate period missing
        // But also check remaining periods and do not alert for them
        issues.clear();
        fakeCounts = List.of(
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(0).getYear(),
                        ATTESTATION_TIME.plusMonths(0).getMonthValue(), 1, 1, 1),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(2).getYear(),
                        ATTESTATION_TIME.plusMonths(2).getMonthValue(), 1, 1, 1)
        );
        coverageCounts.put("TEST", fakeCounts);

        assertFalse(presentCheck.test(contract));

        assertEquals(1, issues.size());
        issues.forEach(issue -> assertTrue(issue.contains("no enrollment found")));
    }

    @DisplayName("Coverage periods have no coverage present")
    @Test
    void whenCoveragePeriodsPresent_passCoveragePresentCheck() {

        Map<String, List<CoverageCount>> coverageCounts = new HashMap<>();
        List<String> issues = new ArrayList<>();
        CoveragePresentCheck presentCheck =
                new CoveragePresentCheck(coverageService, coverageCounts, issues);

        ContractDTO contract = getContractDTO();

        List<CoverageCount> fakeCounts = List.of(
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(0).getYear(),
                        ATTESTATION_TIME.plusMonths(0).getMonthValue(), 1, 1, 1),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(1).getYear(),
                        ATTESTATION_TIME.plusMonths(1).getMonthValue(), 1, 1, 1),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(2).getYear(),
                        ATTESTATION_TIME.plusMonths(2).getMonthValue(), 1, 1, 1)
        );
        coverageCounts.put("TEST", fakeCounts);

        assertTrue(presentCheck.test(contract));
        assertTrue(issues.isEmpty());
    }

    @DisplayName("Coverage changes are limited to 10% between months fails when changes are large")
    @Test
    void whenCoverageUnstable_failCoverageStabilityCheck() {
        Map<String, List<CoverageCount>> coverageCounts = new HashMap<>();
        List<String> issues = new ArrayList<>();
        CoverageStableCheck stableCheck =
                new CoverageStableCheck(coverageService, coverageCounts, issues);

        ContractDTO contract = getContractDTO();

        ZonedDateTime nonDecAttestationTime;
        if (ATTESTATION_TIME.getMonth().getValue() == 12)
            nonDecAttestationTime = ATTESTATION_TIME.plusMonths(1);
        else
            nonDecAttestationTime = ATTESTATION_TIME;

        List<CoverageCount> fakeCounts = List.of(
                new CoverageCount("TEST", nonDecAttestationTime.plusMonths(0).getYear(),
                        nonDecAttestationTime.plusMonths(0).getMonthValue(), 1, 1, 10000),
                new CoverageCount("TEST", nonDecAttestationTime.plusMonths(1).getYear(),
                        nonDecAttestationTime.plusMonths(1).getMonthValue(), 1, 1, 12000),
                new CoverageCount("TEST", nonDecAttestationTime.plusMonths(2).getYear(),
                        nonDecAttestationTime.plusMonths(2).getMonthValue(), 1, 1, 10000)
        );
        coverageCounts.put("TEST", fakeCounts);

        assertFalse(stableCheck.test(contract));

        int expectedIssues = (nonDecAttestationTime.getMonthValue() == 12 || nonDecAttestationTime.plusMonths(1).getMonthValue() == 12
                || nonDecAttestationTime.plusMonths(2).getMonthValue() == 12) && nonDecAttestationTime.getMonthValue() != 0 ? 1 : 2;

        assertEquals(expectedIssues, issues.size());
        issues.forEach(issue -> assertTrue(issue.contains("enrollment changed")));
        assertTrue(issues.get(0).contains("20%"));
    }

    @DisplayName("Coverage changes are limited to 10% between months passes when true or small contract")
    @Test
    void whenCoverageStable_passCoverageStableCheck() {

        Map<String, List<CoverageCount>> coverageCounts = new HashMap<>();
        List<String> issues = new ArrayList<>();
        CoverageStableCheck stableCheck =
                new CoverageStableCheck(coverageService, coverageCounts, issues);

        ContractDTO contract = getContractDTO();

        List<CoverageCount> fakeCounts = List.of(
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(0).getYear(),
                        ATTESTATION_TIME.plusMonths(0).getMonthValue(), 1, 1, 100),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(1).getYear(),
                        ATTESTATION_TIME.plusMonths(1).getMonthValue(), 1, 1, 200),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(2).getYear(),
                        ATTESTATION_TIME.plusMonths(2).getMonthValue(), 1, 1, 1199)
        );
        coverageCounts.put("TEST", fakeCounts);

        assertTrue(stableCheck.test(contract));
        assertTrue(issues.isEmpty());

        fakeCounts = List.of(
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(0).getYear(),
                        ATTESTATION_TIME.plusMonths(0).getMonthValue(), 1, 1, 100_000),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(1).getYear(),
                        ATTESTATION_TIME.plusMonths(1).getMonthValue(), 1, 1, 110_000),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(2).getYear(),
                        ATTESTATION_TIME.plusMonths(2).getMonthValue(), 1, 1, 100_000)
        );
        coverageCounts.put("TEST", fakeCounts);

        assertTrue(stableCheck.test(contract));
        assertTrue(issues.isEmpty());
    }

    @DisplayName("Duplicate enrollment counts cause a failure")
    @Test
    void whenCoverageDuplicated_failCoverageDuplicationCheck() {

        Map<String, List<CoverageCount>> coverageCounts = new HashMap<>();
        List<String> issues = new ArrayList<>();
        CoverageNoDuplicatesCheck duplicatesCheck =
                new CoverageNoDuplicatesCheck(coverageService, coverageCounts, issues);

        ContractDTO contract = getContractDTO();

        List<CoverageCount> fakeCounts = List.of(
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(0).getYear(),
                        ATTESTATION_TIME.plusMonths(0).getMonthValue(), 1, 1, 1),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(1).getYear(),
                        ATTESTATION_TIME.plusMonths(1).getMonthValue(), 2, 1, 1),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(1).getYear(),
                        ATTESTATION_TIME.plusMonths(1).getMonthValue(), 2, 1, 1),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(1).getYear(),
                        ATTESTATION_TIME.plusMonths(1).getMonthValue(), 2, 1, 1),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(2).getYear(),
                        ATTESTATION_TIME.plusMonths(2).getMonthValue(), 3, 1, 1),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(2).getYear(),
                        ATTESTATION_TIME.plusMonths(2).getMonthValue(), 3, 1, 1)
        );
        coverageCounts.put("TEST", fakeCounts);

        assertFalse(duplicatesCheck.test(contract));

        assertEquals(2, issues.size());
        issues.forEach(issue -> assertTrue(issue.contains("sets of enrollment")));
    }

    @DisplayName("No duplicate enrollment found")
    @Test
    void whenCoverageNotDuplicated_passCoverageDuplicatedCheck() {

        Map<String, List<CoverageCount>> coverageCounts = new HashMap<>();
        List<String> issues = new ArrayList<>();
        CoverageNoDuplicatesCheck duplicatesCheck =
                new CoverageNoDuplicatesCheck(coverageService, coverageCounts, issues);

        ContractDTO contract = getContractDTO();

        List<CoverageCount> fakeCounts = List.of(
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(0).getYear(),
                        ATTESTATION_TIME.plusMonths(0).getMonthValue(), 1, 1, 1),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(1).getYear(),
                        ATTESTATION_TIME.plusMonths(1).getMonthValue(), 2, 1, 1),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(2).getYear(),
                        ATTESTATION_TIME.plusMonths(2).getMonthValue(), 3, 1, 1)
        );
        coverageCounts.put("TEST", fakeCounts);

        assertTrue(duplicatesCheck.test(contract));
        assertTrue(issues.isEmpty());
    }

    @DisplayName("Out of date coverage for a coverage period is detected")
    @Test
    void whenCoverageOutOfDate_failCoverageUpToDateCheck() {

        Map<String, List<CoverageCount>> coverageCounts = new HashMap<>();
        List<String> issues = new ArrayList<>();
        CoverageUpToDateCheck upToDateCheck =
                new CoverageUpToDateCheck(coverageService, coverageCounts, issues);

        ContractDTO contract = getContractDTO();

        CoverageSearchEvent event1 = new CoverageSearchEvent();
        event1.setId(1L);

        CoverageSearchEvent event3 = new CoverageSearchEvent();
        event3.setId(3L);

        when(coverageService.findEventWithSuccessfulOffset(anyInt(), anyInt()))
                .thenReturn(Optional.of(event1)).thenReturn(Optional.empty()).thenReturn(Optional.of(event3));
        List<CoverageCount> fakeCounts = List.of(
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(0).getYear(),
                        ATTESTATION_TIME.plusMonths(0).getMonthValue(), 1, 1, 1),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(1).getYear(),
                        ATTESTATION_TIME.plusMonths(1).getMonthValue(), 1, 2, 1),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(2).getYear(),
                        ATTESTATION_TIME.plusMonths(2).getMonthValue(), 1, 3, 1)
        );
        coverageCounts.put("TEST", fakeCounts);

        assertFalse(upToDateCheck.test(contract));

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).contains("no successful search"));

        issues.clear();
        when(coverageService.findEventWithSuccessfulOffset(anyInt(), anyInt()))
                .thenReturn(Optional.of(event1)).thenReturn(Optional.of(event1)).thenReturn(Optional.of(event3));
        assertFalse(upToDateCheck.test(contract));

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).contains("old coverage search"));
    }

    @DisplayName("Up to date enrollment for coverage periods passes checks")
    @Test
    void whenCoverageUpToDate_passCoverageUpToDateCheck() {

        Map<String, List<CoverageCount>> coverageCounts = new HashMap<>();
        List<String> issues = new ArrayList<>();
        CoverageUpToDateCheck upToDateCheck =
                new CoverageUpToDateCheck(coverageService, coverageCounts, issues);

        ContractDTO contract = getContractDTO();

        CoverageSearchEvent event1 = new CoverageSearchEvent();
        event1.setId(1L);

        CoverageSearchEvent event2 = new CoverageSearchEvent();
        event2.setId(2L);

        CoverageSearchEvent event3 = new CoverageSearchEvent();
        event3.setId(3L);

        when(coverageService.findEventWithSuccessfulOffset(anyInt(), anyInt()))
                .thenReturn(Optional.of(event1)).thenReturn(Optional.of(event2)).thenReturn(Optional.of(event3));
        List<CoverageCount> fakeCounts = List.of(
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(0).getYear(),
                        ATTESTATION_TIME.plusMonths(0).getMonthValue(), 1, 1, 1),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(1).getYear(),
                        ATTESTATION_TIME.plusMonths(1).getMonthValue(), 1, 2, 1),
                new CoverageCount("TEST", ATTESTATION_TIME.plusMonths(2).getYear(),
                        ATTESTATION_TIME.plusMonths(2).getMonthValue(), 1, 3, 1)
        );
        coverageCounts.put("TEST", fakeCounts);

        assertTrue(upToDateCheck.test(contract));
        assertTrue(issues.isEmpty());
    }

    @DisplayName("Coverage issues for January->February fails in February")
    @Test
    void whenCoverageAfterFebruary_ThrowsIssueJanuaryCheck() {
        LocalDate currentLocalDate = LocalDate.parse( LocalDate.now().getYear() + "-02-01");
        //override LocalDate.now() to avoid the test pass/failing depending on current date
        try (MockedStatic<LocalDate> topDateTimeUtilMock = Mockito.mockStatic(LocalDate.class)) {
            topDateTimeUtilMock.when(LocalDate::now).thenReturn(currentLocalDate);

            ContractDTO contract = getContractDTO();
            List<CoverageCount> fakeCounts = List.of(
                    new CoverageCount("TEST", 2022,
                            1, 1, 1, 500),
                    new CoverageCount("TEST", 2022,
                            2, 1, 2, 32151),
                    new CoverageCount("TEST", 2022,
                            3, 1, 3, 32100)
            );
            Map<String, List<CoverageCount>> coverageCounts = new HashMap<>();
            List<String> issues = new ArrayList<>();
            coverageCounts.put("TEST", fakeCounts);
            CoverageStableCheck stableCheck =
                    new CoverageStableCheck(coverageService, coverageCounts, issues);
            assertFalse(stableCheck.test(contract));
            assertFalse(issues.isEmpty());
        }
    }

    @DisplayName("Coverage issues for January->February passes in March")
    @Test
    void whenCoverageAfterFebruary_IgnoreIssueJanuaryCheck() {
        LocalDate currentLocalDate = LocalDate.parse( LocalDate.now().getYear() + "-03-01");
        //override LocalDate.now() to avoid the test pass/failing depending on current date
        try (MockedStatic<LocalDate> topDateTimeUtilMock = Mockito.mockStatic(LocalDate.class)) {
            topDateTimeUtilMock.when(LocalDate::now).thenReturn(currentLocalDate);

            ContractDTO contract = getContractDTO();
            List<CoverageCount> fakeCounts = List.of(
                    new CoverageCount("TEST", 2022,
                            1, 1, 1, 500),
                    new CoverageCount("TEST", 2022,
                            2, 1, 2, 32151),
                    new CoverageCount("TEST", 2022,
                            3, 1, 3, 32100)
            );
            Map<String, List<CoverageCount>> coverageCounts = new HashMap<>();
            List<String> issues = new ArrayList<>();
            coverageCounts.put("TEST", fakeCounts);
            CoverageStableCheck stableCheck =
                    new CoverageStableCheck(coverageService, coverageCounts, issues);
            assertTrue(stableCheck.test(contract));
            assertTrue(issues.isEmpty());
        }
    }

}
