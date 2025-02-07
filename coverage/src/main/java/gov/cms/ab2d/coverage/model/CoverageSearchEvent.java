package gov.cms.ab2d.coverage.model;

import gov.cms.ab2d.common.model.TimestampBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bene_coverage_period_id")
    private CoveragePeriod coveragePeriod;

    @Enumerated(EnumType.STRING)
    private CoverageJobStatus oldStatus;

    @Enumerated(EnumType.STRING)
    private CoverageJobStatus newStatus;

    @Column
    private String description;
}
