package gov.cms.ab2d.coverage.model;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Identifiers;
import gov.cms.ab2d.filter.FilterOutByDate;
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
    private final List<FilterOutByDate.DateRange> dateRanges;

    public CoverageSummary(Identifiers identifiers, Contract contract, List<FilterOutByDate.DateRange> dateRanges) {
        this.identifiers = identifiers;
        this.contract = contract;
        this.dateRanges = dateRanges;
    }
}
