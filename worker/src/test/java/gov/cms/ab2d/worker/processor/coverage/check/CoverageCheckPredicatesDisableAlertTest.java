package gov.cms.ab2d.worker.processor.coverage.check;

import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.coverage.model.CoverageCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CoverageCheckPredicatesDisableAlertTest {

    @DisplayName("Coverage periods have no coverage present for S3147 2021 12")
    @Test
    void whenCoveragePeriodsMissingAndContractS3147And2021_12_passCoverageCheck() {
        ContractDTO contractDTO = new ContractDTO("S3147", "Test",
                OffsetDateTime.of(2021, 12, 30, 1, 1, 1, 1, ZoneOffset.UTC)
                        .minus(2, ChronoUnit.YEARS), Contract.ContractType.NORMAL);
        Map<String, List<CoverageCount>> coverageCounts =
                Map.of("S3147",
                        List.of(new CoverageCount("S3147", 2021, 12, 1, 1, 0)));

        List<String> issues = new ArrayList<>();
        new CoveragePresentCheck(null, coverageCounts, issues)
                .test(contractDTO);

        assertTrue(issues.stream()
                .noneMatch(issue -> issue.equals("S3147,2021,12")));
    }

    @DisplayName("Coverage ignored")
    @Test
    void whenCoveragePeriodsMissing_ignore() throws NoSuchMethodException {
        CoveragePresentCheck check = new CoveragePresentCheck(null, null, null);
        Method method = Arrays.stream(CoveragePresentCheck.class.getDeclaredMethods())
                .filter(m -> "ignoreMissing".equals(m.getName()))
                .findFirst()
                .orElse(null);
        method.setAccessible(true);
        assertTrue((Boolean) ReflectionUtils.invokeMethod(method, check, "S3147", 2021, 12));
    }

    @DisplayName("Coverage ignored")
    @Test
    void whenCoveragePeriodsMissing_log() throws NoSuchMethodException {
        ContractDTO contractDTO = new ContractDTO("S3147", "Test",
                OffsetDateTime.of(2021, 12, 30, 1, 1, 1, 1, ZoneOffset.UTC)
                        .minus(2, ChronoUnit.YEARS), Contract.ContractType.NORMAL);
        CoveragePresentCheck check = new CoveragePresentCheck(null, null, null);
        Method method = Arrays.stream(CoveragePresentCheck.class.getDeclaredMethods())
                .filter(m -> "logIssue".equals(m.getName()))
                .findFirst()
                .orElse(null);
        method.setAccessible(true);
        assertSame(ReflectionUtils.invokeMethod(method, check, contractDTO, 2021, 12, null), null);
    }

}
