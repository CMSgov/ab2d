package gov.cms.ab2d.common.model;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Set;

@Entity
@Getter
@Setter
public class Contract {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    @NotNull
    private String contractId;

    private String contractName;

    @ManyToOne
    @JoinColumn(name = "sponsor_id")
    @NotNull
    private Sponsor sponsor;

    @OneToOne(mappedBy = "contract", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Attestation attestation;

    @ManyToMany(mappedBy = "contracts")
    private Set<Beneficiary> beneficiaries;
}
