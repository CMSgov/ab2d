package gov.cms.ab2d.coverage.model;

import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.TimestampBase;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

/**
 * Encapsulates a single contract beneficiary search event triggered by a worker.
 *
 * For IN_PROGRESS searches these events are related via foreign key to actual
 * Coverage information.
 */
@Entity(name = "event_bene_coverage_search_status_change")
@Getter
@Setter
@ToString
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
