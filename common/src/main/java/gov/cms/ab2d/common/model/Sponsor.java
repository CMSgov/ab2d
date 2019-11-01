package gov.cms.ab2d.common.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.Set;

@Entity
@Getter
@Setter
public class Sponsor {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private Integer hpmsId;
    private String orgName;
    private String legalName;

    @ManyToOne
    private Sponsor parent;

    @OneToMany(mappedBy = "sponsor")
    private Set<Attestation> attestations;

}
