package gov.cms.ab2d.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

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
        if (!Objects.equals(contractNumber, otherCount.getContractNumber())) {
            return contractNumber.compareTo(otherCount.getContractNumber());
        }

        if (year != otherCount.getYear()) {
            return year - otherCount.getYear();
        }

        return month - otherCount.getMonth();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoverageCount that = (CoverageCount) o;
        return year == that.year && month == that.month && contractNumber.equals(that.contractNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contractNumber, year, month);
    }
}
