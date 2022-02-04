package gov.cms.ab2d.coverage.model;

import gov.cms.ab2d.filter.FilterOutByDate;
import java.util.List;
import lombok.Getter;
import lombok.ToString;

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
    private final ContractForCoverageDTO contract;
    private final List<FilterOutByDate.DateRange> dateRanges;

    public CoverageSummary(Identifiers identifiers, ContractForCoverageDTO contract, List<FilterOutByDate.DateRange> dateRanges) {
        this.identifiers = identifiers;
        this.contract = contract;
        this.dateRanges = dateRanges;
    }
}
