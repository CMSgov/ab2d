package gov.cms.ab2d.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Information on the difference between the most recently completed coverage search with the previous
 * coverage search.
 */
@Data
@AllArgsConstructor
public class CoverageSearchDiff {

    private CoveragePeriod coveragePeriod;

    private int previousCount;
    private int currentCount;
    private int unchanged;

    /**
     * Previous number of beneficiaries based on last search
     */
    public int getDeletions() {
        return previousCount - unchanged;
    }

    /**
     * Memberships that are unchanged between searches
     */
    public int getAdditions() {
        return currentCount - unchanged;
    }

    @Override
    public String toString() {
        return "CoverageSearchDiff{" +
                coveragePeriod.getContract().getContractNumber() +
                "-" + coveragePeriod.getYear() + "-" + coveragePeriod.getMonth() +
                ", previousCount=" + previousCount +
                ", currentCount=" + currentCount +
                ", unchanged=" + unchanged +
                '}';
    }
}
