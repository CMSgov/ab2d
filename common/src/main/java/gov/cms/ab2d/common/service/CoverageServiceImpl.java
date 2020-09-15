package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Coverage;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearchDiff;
import gov.cms.ab2d.common.model.CoverageSearchEvent;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.CoverageRepository;
import gov.cms.ab2d.common.repository.CoverageSearchEventRepository;
import gov.cms.ab2d.common.repository.CoveragePeriodRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@Transactional
public class CoverageServiceImpl implements CoverageService {

    @Autowired
    private CoverageRepository coverageRepo;

    @Autowired
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Override
    public CoveragePeriod getCoveragePeriod(long contractId, int month, int year) {
        return coveragePeriodRepo.getByContractIdAndMonthAndYear(contractId, month, year);
    }

    @Override
    public boolean isCoveragePeriodInProgress(int searchId) {
        CoveragePeriod period = coveragePeriodRepo.getOne(searchId);
        JobStatus jobStatus = period.getStatus();
        return jobStatus == JobStatus.IN_PROGRESS;
    }

    @Override
    public boolean canEOBSearchBeStarted(int searchId) {
        return !isCoveragePeriodInProgress(searchId);
    }

    @Override
    public JobStatus getSearchStatus(int searchId) {

        CoveragePeriod coverageSearch = coveragePeriodRepo.getOne(searchId);
        return coverageSearch.getStatus();

    }

    @Override
    public Optional<CoverageSearchEvent> findLastEvent(int searchId) {
        return Optional.empty();
    }

    public CoverageSearchEvent getLastEvent(int searchId) {
        return findLastEvent(searchId).orElse(null);
    }

    @Transactional
    @Override
    public CoverageSearchEvent insertCoverage(int searchId, long searchEventId, Collection<String> beneficiaryIds) {

        CoveragePeriod period = coveragePeriodRepo.getOne(searchId);
        CoverageSearchEvent searchEvent = coverageSearchEventRepo.getOne(searchEventId);

        Collection<Coverage> coverages = beneficiaryIds.stream()
                .map(bene -> {
                    Coverage coverage = new Coverage();
                    coverage.setCoveragePeriod(period);
                    coverage.setCoverageSearchEvent(searchEvent);
                    coverage.setBeneficiaryId(bene);

                    return coverage;
                }).collect(toList());

        JobStatus status = getSearchStatus(searchId);
        if (status == JobStatus.IN_PROGRESS) {
            coverageRepo.saveAll(coverages);
        }

        throw new InvalidJobAccessException("cannot update coverages if coverage search is not in progress (current status " + status + " )");
    }

    @Transactional
    @Override
    public void deletePreviousSearch(int searchId) {
        CoveragePeriod period = coveragePeriodRepo.getOne(searchId);
        Optional<CoverageSearchEvent> searchEvent = coverageSearchEventRepo.findSearch(searchId, 1);

        searchEvent.ifPresent(coverageSearchEvent -> coverageRepo.removeAllByCoveragePeriodAndCoverageSearchEvent(period, coverageSearchEvent));
    }

    @Override
    public CoverageSearchDiff searchDiff(int searchId) {

        CoveragePeriod period = coveragePeriodRepo.getOne(searchId);
        Optional<CoverageSearchEvent> previousSearch = coverageSearchEventRepo.findSearch(searchId, 1);
        Optional<CoverageSearchEvent> currentSearch = coverageSearchEventRepo.findSearch(searchId, 0);

        int previousCount = 0;
        if (previousSearch.isPresent()) {
            previousCount = coverageRepo.countByCoverageSearchEvent(previousSearch.get());
        }

        CoverageSearchEvent current = currentSearch.get();
        int currentCount = coverageRepo.countByCoverageSearchEvent(current);

        int unchanged = 0;
        if (previousCount > 0) {
            unchanged = coverageRepo.countIntersection(previousSearch.get().getId(), currentSearch.get().getId());
        }

        return new CoverageSearchDiff(period, previousCount, currentCount, unchanged);
    }

    @Transactional
    @Override
    public CoverageSearchEvent submitCoverageSearch(int searchId, String description) {

        CoveragePeriod period = coveragePeriodRepo.getOne(searchId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus == JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition("cannot change an " + JobStatus.IN_PROGRESS
                    + " contract mapping job to " + JobStatus.SUBMITTED);
        }
        return updateStatus(period, description, JobStatus.SUBMITTED);
    }

    @Transactional
    @Override
    public CoverageSearchEvent startCoverageSearch(int searchId, String description) {

        CoveragePeriod period = coveragePeriodRepo.getOne(searchId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus != JobStatus.SUBMITTED) {
            throw new InvalidJobStateTransition("cannot change from " + period.getStatus()
                    + " to " + JobStatus.IN_PROGRESS);
        }

        return updateStatus(period, description, JobStatus.IN_PROGRESS);
    }

    @Transactional
    @Override
    public CoverageSearchEvent cancelCoverageSearch(int searchId, String description) {

        CoveragePeriod period = coveragePeriodRepo.getOne(searchId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus == JobStatus.FAILED || jobStatus == JobStatus.SUCCESSFUL) {
            throw new InvalidJobStateTransition("cannot change from " + jobStatus
                    + " to " + JobStatus.CANCELLED);
        }

        return updateStatus(period, description, JobStatus.CANCELLED);
    }

    @Transactional
    @Override
    public CoverageSearchEvent failCoverageSearch(int searchId, String description) {

        CoveragePeriod period = coveragePeriodRepo.getOne(searchId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus == JobStatus.CANCELLED || jobStatus == JobStatus.SUCCESSFUL) {
            throw new InvalidJobStateTransition("cannot change from " + jobStatus
                    + " to " + JobStatus.FAILED);
        }

        return updateStatus(period, description, JobStatus.FAILED);
    }

    @Transactional
    @Override
    public CoverageSearchEvent completeCoverageSearch(int searchId, String description) {

        CoveragePeriod period = coveragePeriodRepo.getOne(searchId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus != JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition("cannot change from " + jobStatus
                    + " to " + JobStatus.SUCCESSFUL);
        }

        return updateStatus(period, description, JobStatus.SUCCESSFUL);
    }

    private CoverageSearchEvent updateStatus(CoveragePeriod period, String description, JobStatus status) {
        CoverageSearchEvent currentStatus = getLastEvent(period.getId());

        logStatusChange(period, description, status);

        JobStatus oldStatus = currentStatus != null ? currentStatus.getNewStatus() : null;

        CoverageSearchEvent newStatus = new CoverageSearchEvent();
        newStatus.setCoveragePeriod(period);
        newStatus.setOldStatus(oldStatus);
        newStatus.setNewStatus(status);
        newStatus.setDescription(description);

        period.setStatus(status);

        coveragePeriodRepo.save(period);

        return coverageSearchEventRepo.save(newStatus);
    }

    private void logStatusChange(CoveragePeriod period, String description, JobStatus jobStatus) {
        log.info("Updating job state for search {}-{}-{} from {} to {} due to {}", period.getContract().getId(),
                period.getMonth(), period.getYear(), period.getStatus(), jobStatus, description);
    }
}
