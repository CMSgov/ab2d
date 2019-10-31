package gov.cms.ab2d.common.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;

import static javax.persistence.GenerationType.SEQUENCE;

@Entity
@Getter
@Setter
public class Sponsor {

    @Id
    @GeneratedValue(strategy = SEQUENCE, generator = "sponsor_id_seq")
    @SequenceGenerator(name = "sponsor_id_seq", sequenceName = "sponsor_id_seq", allocationSize = 1)
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
