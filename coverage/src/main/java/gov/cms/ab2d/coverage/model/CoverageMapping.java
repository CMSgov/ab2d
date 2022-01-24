package gov.cms.ab2d.coverage.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

@Getter
public class CoverageMapping {

    private final Set<Identifiers> beneficiaryIds;

    private final CoverageSearchEvent coverageSearchEvent;
    private final CoverageSearchDTO coverageSearchDTO;

    private boolean successful;

    public CoverageMapping(CoverageSearchEvent event, CoverageSearchDTO search) {
        this.coverageSearchEvent = event;
        this.coverageSearchDTO = search;

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
        coverageSearchDTO.incrementAttempts();
        successful = true;
    }

    public void failed() {
        coverageSearchDTO.incrementAttempts();
        successful = false;
    }

    public String getJobId() {
        return "membership-search-" + coverageSearchEvent.getId();
    }
}
