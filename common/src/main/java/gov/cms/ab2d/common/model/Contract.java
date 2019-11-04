package gov.cms.ab2d.common.model;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
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

    private String contractName;

    @OneToMany(mappedBy = "contract")
    private Set<Attestation> attestations = new HashSet<>();

    @ManyToMany(mappedBy = "contracts")
    private Set<Beneficiary> beneficiaries;

}
