package gov.cms.ab2d.worker.processor.coverage.check;

import gov.cms.ab2d.coverage.model.CoverageCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class CoverageCheckPredicatesDisableAlertTest {

    @DisplayName("Coverage periods have no coverage present for S3147 2021 12")
    @Test
    void whenCoveragePeriodsMissingAndContractS3147And2021_12_passCoverageCheck() {

        Map<String, List<CoverageCount>> coverageCounts =
                Map.of("S3147",
                        List.of(new CoverageCount("S3147", 2021, 12, 1, 1, 0)));

        List<String> issues = new ArrayList<>();
        new CoveragePresentCheck(null, coverageCounts, issues);

        assertTrue(issues.stream()
                .noneMatch(issue -> issue.equals("S3147,2021,12")));
    }

}
