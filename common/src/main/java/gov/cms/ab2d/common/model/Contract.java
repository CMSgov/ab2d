package gov.cms.ab2d.common.model;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import java.util.Set;

@Entity
@Getter
@Setter
public class Contract {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String contractId;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
    private Set<Attestation> attestations;

    @ManyToMany(mappedBy = "contracts")
    private Set<Beneficiary> beneficiaries;

}
