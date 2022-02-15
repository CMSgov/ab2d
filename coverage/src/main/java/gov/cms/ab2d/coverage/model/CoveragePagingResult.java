package gov.cms.ab2d.coverage.model;

import lombok.ToString;

import java.util.List;
import java.util.Optional;

/**
 * Result of a coverage paging request that contains all enrollment data for the page of beneficiaries
 * and the request necessary for the next page if another page is present.
 */
@ToString
public class CoveragePagingResult {

    private final CoveragePagingRequest nextRequest;
    private final List<CoverageSummary> coverageSummaries;

    public CoveragePagingResult(List<CoverageSummary> coverageSummaries, CoveragePagingRequest nextRequest) {
        this.coverageSummaries = coverageSummaries;
        this.nextRequest = nextRequest;
    }

    public List<CoverageSummary> getCoverageSummaries() {
        return coverageSummaries;
    }

    public int size() {
        return coverageSummaries.size();
    }

    public Optional<CoveragePagingRequest> getNextRequest() {
        return Optional.ofNullable(nextRequest);
    }
}
