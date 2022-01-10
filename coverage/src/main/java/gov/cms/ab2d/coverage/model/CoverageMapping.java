package gov.cms.ab2d.coverage.model;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.Identifiers;
import lombok.Getter;

import java.util.*;

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

    public Contract getContract() {
        return coverageSearchEvent.getCoveragePeriod().getContract();
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
