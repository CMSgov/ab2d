package gov.cms.ab2d.common.model;

import gov.cms.ab2d.common.util.FilterOutByDate.DateRange;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class CoverageSummary {

    private String beneficiaryId;
    private Contract contract;
    private List<DateRange> dateRanges;
}
