package gov.cms.ab2d.coverage.model;

import gov.cms.ab2d.common.model.CoveragePeriod;
import lombok.*;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Data
public class CoverageDelta {
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

    @Column(name = "beneficiary_id")
    String beneficiary;

    String type;
}