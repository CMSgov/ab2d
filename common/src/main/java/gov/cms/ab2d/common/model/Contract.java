package gov.cms.ab2d.common.model;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;

@Entity
@Getter
@Setter
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String contractId;

    @OneToMany(mappedBy = "contract")
    private Set<Attestation> attestations;

    @ManyToMany(mappedBy = "contracts")
    private Set<Beneficiary> beneficiaries;

}
