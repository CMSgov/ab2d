package gov.cms.ab2d.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

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
    private int month;

    @Column
    private int year;

    @Enumerated(STRING)
    private JobStatus status;
}
