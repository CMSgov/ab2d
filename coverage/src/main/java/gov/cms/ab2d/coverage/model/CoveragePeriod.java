package gov.cms.ab2d.coverage.model;

import gov.cms.ab2d.common.model.TimestampBase;
import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "bene_coverage_period")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class CoveragePeriod extends TimestampBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bene_coverage_period_seq")
    @SequenceGenerator(
        name = "bene_coverage_period_seq",
        sequenceName = "bene_coverage_period_seq",
        allocationSize = 1
    )
    private Integer id;

    @NotNull
    private String contractNumber;

    @Column
    @EqualsAndHashCode.Include
    private int month;

    @Column
    @EqualsAndHashCode.Include
    private int year;

    @Enumerated(EnumType.STRING)
    private CoverageJobStatus status;

    @Column
    private OffsetDateTime lastSuccessfulJob;
}
