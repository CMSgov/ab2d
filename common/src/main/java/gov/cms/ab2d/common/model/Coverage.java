package gov.cms.ab2d.common.model;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Coverage extends TimestampBase {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bene_coverage_search_id")
    private CoverageSearch coverageSearch;

    @ManyToOne
    @JoinColumn(name = "bene_coverage_search_event_id")
    private CoverageSearchEvent coverageSearchEvent;

    @Column
    private String beneficiaryId;

}
