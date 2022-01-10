package gov.cms.ab2d.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

import java.time.OffsetDateTime;


@Entity(name = "bene_coverage_period")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class CoveragePeriod extends TimestampBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "contract_id")
    @EqualsAndHashCode.Include
    private Contract contract;

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
