package gov.cms.ab2d.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CoverageCount {

    private final String contractNumber;
    private final int year;
    private final int month;

    private final int coveragePeriodId;
    private final int coverageEventId;
    private final int beneficiaryCount;

}
