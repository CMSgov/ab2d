package gov.cms.ab2d.common.model;

import lombok.Getter;

import java.util.*;

@Getter
public class CoverageMapping {

    private final Set<String> beneficiaryIds;

    private final CoverageSearchEvent coverageSearchEvent;
    private final CoverageSearch coverageSearch;

    private boolean successful;

    public CoverageMapping(CoverageSearchEvent event, CoverageSearch search) {
        this.coverageSearchEvent = event;
        this.coverageSearch = search;

        beneficiaryIds = new HashSet<>();
    }

    public Contract getContract() {
        return coverageSearchEvent.getCoveragePeriod().getContract();
    }

    public CoveragePeriod getPeriod() {
        return coverageSearchEvent.getCoveragePeriod();
    }

    public int getPeriodId() {
        return coverageSearchEvent.getCoveragePeriod().getId();
    }

    public void addBeneficiaries(Collection<String> beneficiaries) {
        beneficiaryIds.addAll(beneficiaries);
    }

    public void completed() {
        attempt(true);
    }

    public void failed() {
        attempt(false);
    }

    private void attempt(boolean successful) {
        this.coverageSearch.incrementAttempts();
        this.successful = successful;
    }
}
