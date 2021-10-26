package gov.cms.ab2d.common.model;

import lombok.ToString;

import java.util.List;
import java.util.Optional;

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
