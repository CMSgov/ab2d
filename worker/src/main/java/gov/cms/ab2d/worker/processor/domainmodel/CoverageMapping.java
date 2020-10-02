package gov.cms.ab2d.worker.processor.domainmodel;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearchEvent;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Getter
public class CoverageMapping {

    private final CoveragePeriod coveragePeriod;
    private final Set<String> beneficiaryIds;

    private final ArrayList<String> logs;

    private CoverageSearchEvent coverageSearchEvent;
    private int attempts;
    private boolean successful;
    private OffsetDateTime lastAttempt;

    public CoverageMapping(CoveragePeriod coveragePeriod) {
        this.coveragePeriod = coveragePeriod;

        beneficiaryIds = new HashSet<>();
        logs = new ArrayList<>();

        attempts = 0;
        successful = false;
    }

    public Contract getContract() {
        return coveragePeriod.getContract();
    }

    public String getLastLog() {
        return logs.get(logs.size() - 1);
    }

    /**
     * Set the {@link CoverageSearchEvent} to an in progress search event before triggering the callable for each attempt
     * @param event event to tie coverage information back to
     */
    public void setCoverageSearchEvent(CoverageSearchEvent event) {
        this.coverageSearchEvent = event;
    }

    public void addBeneficiaries(Collection<String> beneficiaries) {
        beneficiaryIds.addAll(beneficiaries);
    }

    public void addLog(String logMessage) {
        this.logs.add(logMessage);
    }

    public void completed(String logMessage) {
        attempt(logMessage, true);
    }

    public void failed(String logMessage) {
        attempt(logMessage, false);
    }

    private void attempt(String logMessage, boolean successful) {
        addLog(logMessage);
        this.attempts += 1;
        this.successful = successful;
        this.lastAttempt = OffsetDateTime.now(ZoneId.of("America/New_York"));
    }
}
