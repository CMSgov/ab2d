package gov.cms.ab2d.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "event_bene_coverage_search_status_change")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class CoverageSearchEvent extends TimestampBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bene_coverage_period_id")
    private CoveragePeriod coveragePeriod;

    @Enumerated(EnumType.STRING)
    private JobStatus oldStatus;

    @Enumerated(EnumType.STRING)
    private JobStatus newStatus;

    @Column
    private String description;
}
