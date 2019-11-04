package gov.cms.ab2d.common.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
public class Sponsor {

    @Id
    @GeneratedValue
    private Long id;

    private Integer hpmsId;
    private String orgName;
    private String legalName;

    @ManyToOne
    private Sponsor parent;

    @OneToMany(mappedBy = "sponsor", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Attestation> attestations = new HashSet<>();

    public boolean hasContract(String contractId) {
        for (Attestation attestation : attestations) {
            if (attestation.getContract().getContractId().equals(contractId)) {
                return true;
            }
        }

        return false;
    }
}
