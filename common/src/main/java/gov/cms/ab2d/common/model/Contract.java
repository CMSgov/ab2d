package gov.cms.ab2d.common.model;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;

import static javax.persistence.GenerationType.SEQUENCE;

@Entity
@Getter
@Setter
public class Contract {

    @Id
    @GeneratedValue(strategy = SEQUENCE, generator = "contract_id_seq")
    @SequenceGenerator(name = "contract_id_seq", sequenceName = "contract_id_seq", allocationSize = 1)
    private Long id;

    @Column(unique = true)
    private String contractId;

    @OneToMany(mappedBy = "contract")
    private Set<Attestation> attestations;

    @ManyToMany(mappedBy = "contracts")
    private Set<Beneficiary> beneficiaries;

}
