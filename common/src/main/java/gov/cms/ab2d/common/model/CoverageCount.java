package gov.cms.ab2d.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CoverageCount implements Comparable<CoverageCount> {

    private final String contractNumber;
    private final int year;
    private final int month;

    private final int coveragePeriodId;
    private final int coverageEventId;
    private final int beneficiaryCount;

    @Override
    public int compareTo(CoverageCount otherCount) {
        if (!contractNumber.equals(otherCount.getContractNumber())) {
            return contractNumber.compareTo(otherCount.getContractNumber());
        }

        if (year != otherCount.getYear()) {
            return year - otherCount.getYear();
        }

        return month - otherCount.getMonth();
    }
}
