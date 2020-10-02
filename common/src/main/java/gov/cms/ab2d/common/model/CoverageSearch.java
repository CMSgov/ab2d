package gov.cms.ab2d.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
public class CoverageSearch {
    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne
    @JoinColumn(name = "bene_coverage_period_id", referencedColumnName = "id")
    private CoveragePeriod period;

    // We can use this to search for the earliest search request
    private OffsetDateTime created;

    private int attempts;

    public CoverageSearch() {
        attempts = 0;
        created = OffsetDateTime.now();
    }

    public void incrementAttempts() {
        attempts += 1;
    }
}
