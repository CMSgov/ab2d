package gov.cms.ab2d.coverage.model.v3;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class CoverageV3Count implements Comparable<CoverageV3Count> {

    private final String contractNumber;
    private final int year;
    private final int month;
    private final int beneficiaryCount;

    @Override
    public int compareTo(CoverageV3Count otherCount) {
        if (!Objects.equals(contractNumber, otherCount.getContractNumber())) {
            return contractNumber.compareTo(otherCount.getContractNumber());
        }

        if (year != otherCount.getYear()) {
            return year - otherCount.getYear();
        }

        return month - otherCount.getMonth();
    }
}
