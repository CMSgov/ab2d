package gov.cms.ab2d.coverage.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Entity
@Data
@AllArgsConstructor
public class CoverageSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "coverage_search_seq")
    @SequenceGenerator(name = "coverage_search_seq", sequenceName = "public.coverage_search_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne
    @JoinColumn(name = "bene_coverage_period_id", referencedColumnName = "id")
    private CoveragePeriod period;

    // We can use this to search for the earliest search request
    @EqualsAndHashCode.Exclude  // id and period are sufficient, breaks on Windows due sub-seconds not matching
    private OffsetDateTime created;

    /**
     * Number of attempts made to retrieve membership information from BFD. This is used to create a circuit breaker
     * where a configured number of failures to retrieve membership information from BFD will trigger an exception
     * and warning.
     *
     * When a search against BFD fails this number of attempts is incremented and saved.
     */
    private int attempts;

    public CoverageSearch() {
        attempts = 0;
        created = OffsetDateTime.now();
    }

    public void incrementAttempts() {
        attempts += 1;
    }
}
