package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.CoverageSearchEventRepository;
import gov.cms.ab2d.common.repository.CoverageSearchRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

/**
 * Use for performance testing the {@link Coverage} tables performance with large inserts, selects, and deletes
 * done by the {@link CoverageService}
 *
 * Note: this job does not clean up after itself and anticipates artifacts being wiped between tests
 */
public class InsertionJob implements Callable<CoverageSearchEvent> {

    private final CoveragePeriod period;
    private final DataSource dataSource;
    private final CoverageService coverageService;
    private final CoverageSearchRepository coverageSearchRepository;
    private final int dataPoints;
    private final int experiments;
    public static final int CHUNK_SIZE = 50000;

    public static class BeneficiaryIdSupplier implements Supplier<Identifiers> {

        private int id = 0;

        public BeneficiaryIdSupplier() {}

        public Identifiers get() {
            int generated = id++;
            return new Identifiers("test-" + generated, "mbi-" + generated);
        }
    }

    /**
     *
     * @param period period that all beneficiaries are active for
     * @param dataSource driver to build sql templates off of for deletes
     * @param coverageService service to use for inserts
     * @param dataPoints number of beneficiaries to insert
     * @param experiments number of times to repeat insertion
     */
    public InsertionJob(CoveragePeriod period, DataSource dataSource, CoverageService coverageService,
                        int dataPoints, int experiments, CoverageSearchRepository coverageSearchRepository) {
        this.period = period;
        this.dataSource = dataSource;
        this.coverageService = coverageService;
        this.dataPoints = dataPoints;
        this.experiments = experiments;
        this.coverageSearchRepository = coverageSearchRepository;
    }

    public CoverageSearchEvent call() {
        // Add in progress event as foreign key for all inserts
        coverageService.submitSearch(period.getId(), "testing");
        Optional<CoverageSearch> search = coverageSearchRepository.findFirstByOrderByCreatedAsc();
        CoverageSearchEvent inProgress = coverageService.startSearch(search.get(), "testing").get().getCoverageSearchEvent();

        // Run inserts
        // If number of experiments is greater than 1 then data will be erased after each experiment
        List<Long> timings = performExperiments(period, inProgress, dataPoints, experiments);

        // Average and print times of experiments
        long averageTime = timings.stream().reduce(0L, Long::sum) / timings.size();
        System.out.println("Average milliseconds " + averageTime);
        System.out.println("Times " + timings.stream().map(Object::toString).collect(joining(", ")));

        return inProgress;
    }

    /**
     * Perform experiments and capture timing of batch inserts
     */
    private List<Long> performExperiments(CoveragePeriod period, CoverageSearchEvent inProgress, int dataPoints, int experiments) {
        BeneficiaryIdSupplier supplier = new BeneficiaryIdSupplier();

        List<Long> timings = new ArrayList<>();

        for (int experiment = 0; experiment < experiments; experiment++) {

            Instant start = Instant.now();
            conductBatchInserts(supplier, inProgress.getId(), dataPoints);
            Instant end = Instant.now();

            // Only wipe data between tests if more than one experiment is being performed
            if (experiments > 1) {
                deleteCoverage();
            }

            timings.add(Duration.between(start, end).toMillis());
        }
        return timings;
    }

    /**
     * Insert records in batches until dataPoints records have been inserted
     * @param supplier id supplier giving beneficiary ids
     * @param searchEventId search event id which is foreign key for coverage
     * @param dataPoints list of data points to insert
     */
    private void conductBatchInserts(BeneficiaryIdSupplier supplier, long searchEventId, int dataPoints) {

        for (int written = 0; written < dataPoints; written += CHUNK_SIZE) {
            List<Identifiers> batch = new ArrayList<>(CHUNK_SIZE);
            IntStream.iterate(0, i -> i + 1).limit(CHUNK_SIZE).forEach(i -> batch.add(supplier.get()));

            coverageService.insertCoverage(searchEventId, new HashSet<>(batch));
        }
    }

    // Delete all rows in coverage table
    private void deleteCoverage() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM coverage")) {
            statement.execute();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

}
