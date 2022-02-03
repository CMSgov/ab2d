package gov.cms.ab2d.coverage.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

@Getter
public class CoverageMapping {

    private final Set<Identifiers> beneficiaryIds;

    private final CoverageSearchEvent coverageSearchEvent;
    private final CoverageSearch coverageSearch;

    private boolean successful;

    public CoverageMapping(CoverageSearchEvent event, CoverageSearch search) {
        this.coverageSearchEvent = event;
        this.coverageSearch = search;

        beneficiaryIds = new HashSet<>();
    }

    public String getContractNumber() {
        return coverageSearchEvent.getCoveragePeriod().getContractNumber();
    }

    public CoveragePeriod getPeriod() {
        return coverageSearchEvent.getCoveragePeriod();
    }

    public int getPeriodId() {
        return coverageSearchEvent.getCoveragePeriod().getId();
    }

    public void addBeneficiaries(Collection<Identifiers> beneficiaries) {
        beneficiaryIds.addAll(beneficiaries);
    }

    public void completed() {
        coverageSearch.incrementAttempts();
        successful = true;
    }

    public void failed() {
        coverageSearch.incrementAttempts();
        successful = false;
    }

    public String getJobId() {
        return "membership-search-" + coverageSearchEvent.getId();
    }
}
