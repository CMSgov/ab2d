package gov.cms.ab2d.domain;


import javax.persistence.*;
import java.util.Set;

@Entity
public class Contract {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String contractId;

    @OneToMany(mappedBy = "contract")
    private Set<Attestation> attestations;

    @ManyToMany(mappedBy = "contracts")
    private Set<Beneficiary> beneficiaries;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public Set<Attestation> getAttestations() {
        return attestations;
    }

    public void setAttestations(Set<Attestation> attestations) {
        this.attestations = attestations;
    }

    public Set<Beneficiary> getBeneficiaries() {
        return beneficiaries;
    }

    public void setBeneficiaries(Set<Beneficiary> beneficiaries) {
        this.beneficiaries = beneficiaries;
    }


}
