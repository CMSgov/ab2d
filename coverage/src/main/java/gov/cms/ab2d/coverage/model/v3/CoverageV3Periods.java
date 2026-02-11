package gov.cms.ab2d.coverage.model.v3;

import gov.cms.ab2d.coverage.model.YearMonthRecord;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class CoverageV3Periods {
    private final List<YearMonthRecord> historicalCoverage;
    private final List<YearMonthRecord> recentCoverage;
}
