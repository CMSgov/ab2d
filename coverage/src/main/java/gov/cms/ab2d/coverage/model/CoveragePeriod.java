package gov.cms.ab2d.coverage.model;

import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
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
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
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
    private JobStatus status;

    @Column
    private OffsetDateTime lastSuccessfulJob;
}
