package gov.cms.ab2d.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import static javax.persistence.EnumType.STRING;

@Entity(name = "bene_coverage_search")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CoverageSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @EqualsAndHashCode.Include
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Column
    @NotNull
    private Integer month;

    @Column
    @NotNull
    private Integer year;

    @Enumerated(STRING)
    private JobStatus status;
}
