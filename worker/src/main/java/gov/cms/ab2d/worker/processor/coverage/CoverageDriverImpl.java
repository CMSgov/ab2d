package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageMapping;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearch;
import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.service.CoverageService;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.DateUtil;
import gov.cms.ab2d.worker.config.CoverageUpdateConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static gov.cms.ab2d.common.util.DateUtil.getAB2DEpoch;

@Slf4j
public class CoverageDriverImpl implements CoverageDriver {

    private static final long SIXTY_SECONDS = 60000;

    private final ContractService contractService;
    private final CoverageService coverageService;
    private final CoverageProcessor coverageProcessor;
    private final CoverageUpdateConfig config;
    private final CoverageLockWrapper coverageLockWrapper;
    private final PropertiesService propertiesService;

    public CoverageDriverImpl(ContractService contractService, CoverageService coverageService,
                              PropertiesService propertiesService, CoverageProcessor coverageProcessor,
                              CoverageUpdateConfig coverageUpdateConfig, CoverageLockWrapper coverageLockWrapper) {
        this.contractService = contractService;
        this.coverageService = coverageService;
        this.coverageProcessor = coverageProcessor;
        this.config = coverageUpdateConfig;
        this.coverageLockWrapper = coverageLockWrapper;
        this.propertiesService = propertiesService;
    }

    /**
     * Find all work that needs to be done including new coverage periods, jobs that have been running too long,
     * and coverage information that is too old.
     */
    @Override
    public void queueStaleCoveragePeriods() {

        log.info("attempting to find all stale coverage periods");

        // Use a linked hash set to order by discovery
        // Add all new coverage periods that have never been mapped/searched
        Set<CoveragePeriod> outOfDateInfo = new LinkedHashSet<>(coverageService.coveragePeriodNeverSearchedSuccessfully());

        // Find all stuck coverage searches and cancel them
        outOfDateInfo.addAll(findAndCancelStuckCoverageJobs());

        // For all months into the past find old coverage searches
        outOfDateInfo.addAll(findStaleCoverageInformation());

        log.info("queueing all stale coverage periods");

        for (CoveragePeriod period : outOfDateInfo) {
            coverageProcessor.queueCoveragePeriod(period, false);
        }

        log.info("queued all stale coverage periods");
    }

    /**
     * Discover any nonexistent coverage periods and add them to the list of coverage periods
     */
    @Override
    public void discoverCoveragePeriods() {

        log.info("discovering all coverage periods that should exist");

        List<Contract> attestedContracts = contractService.getAllAttestedContracts();

        ZonedDateTime now = ZonedDateTime.now();

        ZonedDateTime epoch = getAB2DEpoch();

        for (Contract contract : attestedContracts) {
            ZonedDateTime attestationTime = contract.getESTAttestationTime();

            // Force first coverage period to be after
            // January 1st 2020 which is the first moment we report data for
            if (attestationTime.isBefore(epoch)) {
                log.debug("contract attested before ab2d epoch setting to epoch");
                attestationTime = getAB2DEpoch();
            }

            int coveragePeriodsForContracts = 0;
            while (attestationTime.isBefore(now)) {
                coverageService.getCreateIfAbsentCoveragePeriod(contract, attestationTime.getMonthValue(), attestationTime.getYear());
                coveragePeriodsForContracts += 1;

                attestationTime = attestationTime.plusMonths(1);
            }

            log.info("discovered {} coverage periods for contract {}", coveragePeriodsForContracts,
                    contract.getContractName());
        }

        log.info("discovered all coverage periods now exiting");
    }

    private Set<CoveragePeriod> findAndCancelStuckCoverageJobs() {

        log.info("attempting to find all stuck coverage searches and then cancel those stuck coverage searches");

        Set<CoveragePeriod> stuckJobs = new LinkedHashSet<>(
                coverageService.coveragePeriodStuckJobs(OffsetDateTime.now(ZoneOffset.UTC).minusHours(config.getStuckHours())));

        for (CoveragePeriod period : stuckJobs) {
            coverageService.failSearch(period.getId(), "coverage period current job has been stuck for at least "
                    + config.getStuckHours() + " hours and is now considered failed.");
        }

        return stuckJobs;
    }

    private Set<CoveragePeriod> findStaleCoverageInformation() {

        log.info("attempting to find all coverage information that is out of date and reduce down to coverage periods");

        Set<CoveragePeriod> stalePeriods = new LinkedHashSet<>();
        int monthsInPast = 0;
        OffsetDateTime dateTime = OffsetDateTime.now(DateUtil.AB2D_ZONE);

        do {
            // Get past month and year
            OffsetDateTime pastMonthTime = dateTime.minusMonths(monthsInPast);
            int month = pastMonthTime.getMonthValue();
            int year = pastMonthTime.getYear();

            // Look for coverage periods that have not been updated
            long daysInPast = config.getStaleDays() * (monthsInPast + 1);
            OffsetDateTime lastUpdatedAfter = dateTime.minusDays(daysInPast);

            stalePeriods.addAll(coverageService.coveragePeriodNotUpdatedSince(month, year, lastUpdatedAfter));
            monthsInPast++;
        } while (monthsInPast < config.getPastMonthsToUpdate());

        return stalePeriods;
    }

    @Scheduled(fixedDelay = SIXTY_SECONDS, initialDelayString = "${coverage.update.initial.delay}")
    public void loadMappingJob() {

        if (propertiesService.isInMaintenanceMode()) {
            log.debug("waiting to execute queued coverage searches because api is in maintenance mode");
        }

        if (coverageProcessor.isProcessorBusy()) {
            log.debug("not starting any new coverage mapping jobs because service is full.");
            return;
        }

        while (!coverageProcessor.isProcessorBusy()) {
            Optional<CoverageSearch> search = coverageLockWrapper.getNextSearch();
            if (search.isEmpty()) {
                break;
            }

            Optional<CoverageMapping> maybeSearch = coverageService.startSearch(search.get(), "starting a job");
            if (maybeSearch.isEmpty()) {
                break;
            }

            CoverageMapping mapping = maybeSearch.get();

            log.debug("found a search in queue for contract {} during {}-{}, attempting to search",
                    mapping.getContract().getContractNumber(), mapping.getPeriod().getMonth(),
                    mapping.getPeriod().getYear());

            if (!coverageProcessor.startJob(mapping)) {
                coverageService.cancelSearch(mapping.getPeriodId(), "failed to start job");
                coverageProcessor.queueMapping(mapping, false);
            }
        }
    }
}
