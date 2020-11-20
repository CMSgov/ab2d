package gov.cms.ab2d.common.model;

import gov.cms.ab2d.common.util.FilterOutByDate.DateRange;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * Summary of coverage membership for a specific contract over a set of date ranges.
 *
 * This does not necessarily reflect a search over the entire lifetime of a contract only
 * whatever date ranges are included.
 */
@Getter
@ToString
public class CoverageSummary {

    private final Identifiers identifiers;
    private final Contract contract;
    private final List<DateRange> dateRanges;

    public CoverageSummary(String beneficiaryId, String mbi, Contract contract, List<DateRange> dateRanges) {
        this.identifiers = new Identifiers(beneficiaryId, mbi);
        this.contract = contract;
        this.dateRanges = dateRanges;
    }
}
