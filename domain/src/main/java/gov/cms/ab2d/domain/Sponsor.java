package gov.cms.ab2d.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;

@Entity
@Getter
@Setter
public class Sponsor {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String sponsorID;
    private String name;

    @OneToMany(mappedBy = "sponsor")
    private Set<Attestation> attestations;

}
