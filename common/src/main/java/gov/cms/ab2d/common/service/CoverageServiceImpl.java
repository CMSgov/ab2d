package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Coverage;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearchDiff;
import gov.cms.ab2d.common.model.CoverageSearchEvent;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.CoveragePeriodRepository;
import gov.cms.ab2d.common.repository.CoverageRepository;
import gov.cms.ab2d.common.repository.CoverageSearchEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Optional;

import static gov.cms.ab2d.common.util.Constants.AB2D_EPOCH;
import static java.util.stream.Collectors.toList;

/**
 * Interact with Coverage, CoveragePeriod, and CoverageSearchEvent entities/tables including
 * bulk insertions, reads, and deletes.
 *
 * Warning: {@link org.springframework.data.jpa.repository.JpaRepository#getOne(Object)}
 * loads lazily which causes issues when an object is returned by the {@link CoverageServiceImpl}
 * to another context which may not have a {@link Transactional} context.
 *
 * To prevent issues getOne is not used in this service at all.
 *
 * TODO: bulk inserts, selects, and deletes using plain jdbc
 */
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
        checkMonthAndYear(month, year);
        return coveragePeriodRepo.getByContractIdAndMonthAndYear(contractId, month, year);
    }

    @Override
    public boolean isCoveragePeriodInProgress(int searchId) {
        CoveragePeriod period = findCoveragePeriod(searchId);
        JobStatus jobStatus = period.getStatus();
        return jobStatus == JobStatus.IN_PROGRESS;
    }

    @Override
    public boolean canEOBSearchBeStarted(int searchId) {
        return !isCoveragePeriodInProgress(searchId);
    }

    @Override
    public JobStatus getSearchStatus(int searchId) {
        CoveragePeriod coverageSearch = findCoveragePeriod(searchId);
        return coverageSearch.getStatus();

    }

    @Override
    public Optional<CoverageSearchEvent> findLastEvent(int searchId) {
        CoveragePeriod period = findCoveragePeriod(searchId);
        return findLastEvent(period);
    }

    private Optional<CoverageSearchEvent> findLastEvent(CoveragePeriod period) {
        return coverageSearchEventRepo.findFirstByCoveragePeriodOrderByCreatedDesc(period);
    }

    private CoverageSearchEvent getLastEvent(CoveragePeriod period) {
        return findLastEvent(period).orElse(null);
    }

    @Override
    public CoverageSearchEvent insertCoverage(int searchId, long searchEventId, Collection<String> beneficiaryIds) {

        CoveragePeriod period = findCoveragePeriod(searchId);
        CoverageSearchEvent searchEvent = findCoverageSearchEvent(searchEventId);

        Collection<Coverage> coverages = beneficiaryIds.stream()
                .map(bene -> buildCoverage(period, searchEvent, bene)).collect(toList());

        JobStatus status = getSearchStatus(searchId);
        if (status == JobStatus.IN_PROGRESS) {
            coverageRepo.saveAll(coverages);
        } else {
            throw new InvalidJobAccessException("cannot update coverages if coverage search is not in progress (current status " + status + " )");
        }

        return searchEvent;
    }

    private Coverage buildCoverage(CoveragePeriod period, CoverageSearchEvent searchEvent, String bene) {
        Coverage coverage = new Coverage();
        coverage.setCoveragePeriod(period);
        coverage.setCoverageSearchEvent(searchEvent);
        coverage.setBeneficiaryId(bene);

        return coverage;
    }

    @Override
    public void deletePreviousSearch(int searchId) {
        CoveragePeriod period = findCoveragePeriod(searchId);
        Optional<CoverageSearchEvent> searchEvent = coverageSearchEventRepo.findSearchWithOffset(searchId, 1);

        searchEvent.ifPresent(coverageSearchEvent ->
                coverageRepo.removeAllByCoveragePeriodAndCoverageSearchEvent(period, coverageSearchEvent));
    }

    @Override
    public CoverageSearchDiff searchDiff(int searchId) {

        CoveragePeriod period = findCoveragePeriod(searchId);

        if (period.getStatus() != JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition("Cannot diff a currently running search against previous search because results may be added");
        }

        Optional<CoverageSearchEvent> previousSearch = coverageSearchEventRepo.findSearchWithOffset(searchId, 1);
        Optional<CoverageSearchEvent> currentSearch = coverageSearchEventRepo.findSearchWithOffset(searchId, 0);

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

    @Override
    public CoverageSearchEvent submitCoverageSearch(int searchId, String description) {

        CoveragePeriod period = findCoveragePeriod(searchId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus == JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition("cannot change an " + JobStatus.IN_PROGRESS
                    + " contract mapping job to " + JobStatus.SUBMITTED);
        }
        return updateStatus(period, description, JobStatus.SUBMITTED);
    }

    @Override
    public CoverageSearchEvent startCoverageSearch(int searchId, String description) {

        CoveragePeriod period = findCoveragePeriod(searchId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus != JobStatus.SUBMITTED) {
            throw new InvalidJobStateTransition("cannot change from " + period.getStatus()
                    + " to " + JobStatus.IN_PROGRESS);
        }

        return updateStatus(period, description, JobStatus.IN_PROGRESS);
    }

    @Override
    public CoverageSearchEvent cancelCoverageSearch(int searchId, String description) {

        CoveragePeriod period = findCoveragePeriod(searchId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus != JobStatus.SUBMITTED) {
            throw new InvalidJobStateTransition("cannot change from " + jobStatus
                    + " to " + JobStatus.CANCELLED);
        }

        return updateStatus(period, description, JobStatus.CANCELLED);
    }

    @Override
    public CoverageSearchEvent failCoverageSearch(int searchId, String description) {

        CoveragePeriod period = findCoveragePeriod(searchId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus != JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition("cannot change from " + jobStatus
                    + " to " + JobStatus.FAILED);
        }

        return updateStatus(period, description, JobStatus.FAILED);
    }

    @Override
    public CoverageSearchEvent completeCoverageSearch(int searchId, String description) {

        CoveragePeriod period = findCoveragePeriod(searchId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus != JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition("cannot change from " + jobStatus
                    + " to " + JobStatus.SUCCESSFUL);
        }

        return updateStatus(period, description, JobStatus.SUCCESSFUL);
    }

    private static void checkMonthAndYear(int month, int year) {
        if (month < 1 || month > 12) {
            final String errMsg = "invalid value for month. Month must be between 1 and 12";
            log.error("{} - invalid month :[{}]", errMsg, month);
            throw new IllegalArgumentException(errMsg);
        }

        OffsetDateTime time = OffsetDateTime.now(ZoneOffset.UTC);
        int currentYear = time.getYear();

        if (year < AB2D_EPOCH || year > currentYear) {
            final String errMsg = "invalid value for year. Year must be between " + AB2D_EPOCH + " and " + currentYear;
            log.error("{} - invalid year :[{}]", errMsg, year);
            throw new IllegalArgumentException(errMsg);
        }
    }

    private CoverageSearchEvent updateStatus(CoveragePeriod period, String description, JobStatus status) {
        CoverageSearchEvent currentStatus = getLastEvent(period);

        logStatusChange(period, description, status);

        JobStatus oldStatus = currentStatus != null ? currentStatus.getNewStatus() : null;

        CoverageSearchEvent newStatus = new CoverageSearchEvent();
        newStatus.setCoveragePeriod(period);
        newStatus.setOldStatus(oldStatus);
        newStatus.setNewStatus(status);
        newStatus.setDescription(description);

        period.setStatus(status);

        coveragePeriodRepo.saveAndFlush(period);

        return coverageSearchEventRepo.saveAndFlush(newStatus);
    }

    private void logStatusChange(CoveragePeriod period, String description, JobStatus jobStatus) {
        log.info("Updating job state for search {}-{}-{} from {} to {} due to {}", period.getContract().getId(),
                period.getMonth(), period.getYear(), period.getStatus(), jobStatus, description);
    }

    /**
     * Eagerly load a {@link CoveragePeriod} by id.
     * @param searchId coverage period id to load
     * @return coverage period if found
     * @throws ResourceNotFoundException if coverage period is not found
     */
    private CoveragePeriod findCoveragePeriod(int searchId) {
        return coveragePeriodRepo.findById(searchId)
                .orElseThrow(() -> new ResourceNotFoundException("coverage period with id not found"));
    }

    private CoverageSearchEvent findCoverageSearchEvent(long searchId) {
        return coverageSearchEventRepo.findById(searchId)
                .orElseThrow(() -> new ResourceNotFoundException("coverage period with id not found"));
    }
}
