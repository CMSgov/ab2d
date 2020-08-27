package gov.cms.ab2d.common.model;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Contract {
    public enum ContractState { UNATTESTED, VALID, ATTESTATION_WITHDRAWN, DELETED }

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true)
    @NotNull
    private String contractNumber;

    private String contractName;

    @ManyToOne
    @JoinColumn(name = "sponsor_id")
    @NotNull
    private Sponsor sponsor;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime attestedOn;

    @OneToMany(mappedBy = "contract")
    private Set<Coverage> coverages = new HashSet<>();

    @Transient
    private ContractState contractState;

    public boolean isDeleted() {
        return ContractState.DELETED == contractState;
    }

    public void markDeleted() {
        contractState = ContractState.DELETED;
    }
}
