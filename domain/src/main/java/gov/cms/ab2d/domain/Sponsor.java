package gov.cms.ab2d.domain;

import javax.persistence.*;
import java.util.Set;

@Entity
public class Sponsor {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String sponsorID;
    private String name;

    @OneToMany(mappedBy = "sponsor")
    private Set<Attestation> attestations;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSponsorID() {
        return sponsorID;
    }

    public void setSponsorID(String sponsorID) {
        this.sponsorID = sponsorID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Attestation> getAttestations() {
        return attestations;
    }

    public void setAttestations(Set<Attestation> attestations) {
        this.attestations = attestations;
    }


}
