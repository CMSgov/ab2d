package gov.cms.ab2d.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity(name = "event_bene_coverage_search_status_change")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CoverageSearchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bene_coverage_period_id")
    private CoveragePeriod coveragePeriod;

    @Column(name = "time_of_event")
    private OffsetDateTime occuredAt;

    @Enumerated(EnumType.STRING)
    private JobStatus oldStatus;

    @Enumerated(EnumType.STRING)
    private JobStatus newStatus;

    @Column
    private String description;
}
