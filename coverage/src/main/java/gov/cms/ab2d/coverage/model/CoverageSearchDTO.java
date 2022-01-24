package gov.cms.ab2d.coverage.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Entity
@Data
@AllArgsConstructor
public class CoverageSearchDTO {
    @Id
    @GeneratedValue
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

    public CoverageSearchDTO() {
        attempts = 0;
        created = OffsetDateTime.now();
    }

    public void incrementAttempts() {
        attempts += 1;
    }
}
