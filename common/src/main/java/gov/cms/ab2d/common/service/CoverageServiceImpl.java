package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.CoverageRepository;
import gov.cms.ab2d.common.repository.CoverageSearchEventRepository;
import gov.cms.ab2d.common.repository.CoverageSearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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
    private CoverageSearchRepository coverageSearchRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    private JobService jobService;

    @Override
    public CoverageSearch getCoverageSearch(long contractId, int month, int year) {
        return coverageSearchRepo.getByContractIdAndMonthAndYear(contractId, month, year);
    }

    @Override
    public boolean isCoverageSearchInProgress(int searchId) {
        CoverageSearch coverageSearch = coverageSearchRepo.getOne(searchId);
        JobStatus jobStatus = coverageSearch.getStatus();
        return jobStatus == JobStatus.IN_PROGRESS;
    }

    @Override
    public boolean canEOBSearchBeStarted(int searchId) {
        return !isCoverageSearchInProgress(searchId);
    }

    @Override
    public JobStatus getSearchStatus(int searchId) {

        CoverageSearch coverageSearch = coverageSearchRepo.getOne(searchId);
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

        CoverageSearch search = coverageSearchRepo.getOne(searchId);
        CoverageSearchEvent searchEvent = coverageSearchEventRepo.getOne(searchEventId);

        Collection<Coverage> coverages = beneficiaryIds.stream()
                .map(bene -> {
                    Coverage coverage = new Coverage();
                    coverage.setCoverageSearch(search);
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
        CoverageSearch search = coverageSearchRepo.getOne(searchId);
        Optional<CoverageSearchEvent> searchEvent = coverageSearchEventRepo.findSearch(searchId, 1);

        searchEvent.ifPresent(coverageSearchEvent -> coverageRepo.removeAllByCoverageSearchAndCoverageSearchEvent(search, coverageSearchEvent));
    }

    @Override
    public CoverageSearchDiff searchDiff(int searchId) {

        CoverageSearch search = coverageSearchRepo.getOne(searchId);
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

        return new CoverageSearchDiff(search, previousCount, currentCount, unchanged);
    }

    @Transactional
    @Override
    public CoverageSearchEvent submitCoverageSearch(int searchId, String description) {

        CoverageSearch coverageSearch = coverageSearchRepo.getOne(searchId);
        JobStatus jobStatus = coverageSearch.getStatus();

        if (jobStatus == JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition("cannot change an " + JobStatus.IN_PROGRESS
                    + " contract mapping job to " + JobStatus.SUBMITTED);
        }
        return updateStatus(coverageSearch, description, JobStatus.SUBMITTED);
    }

    @Transactional
    @Override
    public CoverageSearchEvent startCoverageSearch(int searchId, String description) {

        CoverageSearch coverageSearch = coverageSearchRepo.getOne(searchId);
        JobStatus jobStatus = coverageSearch.getStatus();

        if (jobStatus != JobStatus.SUBMITTED) {
            throw new InvalidJobStateTransition("cannot change from " + coverageSearch.getStatus()
                    + " to " + JobStatus.IN_PROGRESS);
        }

        return updateStatus(coverageSearch, description, JobStatus.IN_PROGRESS);
    }

    @Transactional
    @Override
    public CoverageSearchEvent cancelCoverageSearch(int searchId, String description) {

        CoverageSearch coverageSearch = coverageSearchRepo.getOne(searchId);
        JobStatus jobStatus = coverageSearch.getStatus();

        if (jobStatus == JobStatus.FAILED || jobStatus == JobStatus.SUCCESSFUL) {
            throw new InvalidJobStateTransition("cannot change from " + jobStatus
                    + " to " + JobStatus.CANCELLED);
        }

        return updateStatus(coverageSearch, description, JobStatus.CANCELLED);
    }

    @Transactional
    @Override
    public CoverageSearchEvent failCoverageSearch(int searchId, String description) {

        CoverageSearch coverageSearch = coverageSearchRepo.getOne(searchId);
        JobStatus jobStatus = coverageSearch.getStatus();

        if (jobStatus == JobStatus.CANCELLED || jobStatus == JobStatus.SUCCESSFUL) {
            throw new InvalidJobStateTransition("cannot change from " + jobStatus
                    + " to " + JobStatus.FAILED);
        }

        return updateStatus(coverageSearch, description, JobStatus.FAILED);
    }

    @Transactional
    @Override
    public CoverageSearchEvent completeCoverageSearch(int searchId, String description) {

        CoverageSearch coverageSearch = coverageSearchRepo.getOne(searchId);
        JobStatus jobStatus = coverageSearch.getStatus();

        if (jobStatus != JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition("cannot change from " + jobStatus
                    + " to " + JobStatus.SUCCESSFUL);
        }

        return updateStatus(coverageSearch, description, JobStatus.SUCCESSFUL);
    }

    private CoverageSearchEvent updateStatus(CoverageSearch coverageSearch, String description, JobStatus status) {
        CoverageSearchEvent currentStatus = getLastEvent(coverageSearch.getId());

        logStatusChange(coverageSearch, description, status);

        JobStatus oldStatus = currentStatus != null ? currentStatus.getNewStatus() : null;

        CoverageSearchEvent newStatus = new CoverageSearchEvent();
        newStatus.setCoverageSearch(coverageSearch);
        newStatus.setOccuredAt(OffsetDateTime.now());
        newStatus.setOldStatus(oldStatus);
        newStatus.setNewStatus(status);
        newStatus.setDescription(description);

        coverageSearch.setStatus(status);

        coverageSearchRepo.save(coverageSearch);

        return coverageSearchEventRepo.save(newStatus);
    }

    private void logStatusChange(CoverageSearch search, String description, JobStatus jobStatus) {
        log.info("Updating job state for search {}-{}-{} from {} to {} due to {}", search.getContract().getId(),
                search.getMonth(), search.getYear(), search.getStatus(), jobStatus, description);
    }
}
