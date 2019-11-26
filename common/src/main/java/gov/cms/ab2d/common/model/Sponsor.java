package gov.cms.ab2d.common.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
public class Sponsor {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    private Integer hpmsId;

    @NotNull
    private String orgName;
    private String legalName;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "parent_id")
    private Sponsor parent;

    @OneToMany(mappedBy = "parent")
    private Set<Sponsor> children = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL,
            mappedBy = "sponsor",
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private Set<Contract> contracts = new HashSet<>();

    public boolean hasContract(String contractNum) {
        for (Contract contract : contracts) {
            if (contractNum.equalsIgnoreCase(contract.getContractNumber())) {
                return true;
            }
        }

        return false;
    }



    public List<Contract> getAggregatedAttestedContracts() {
        return parent == null
                ? getAttestedContractsOfChildren()
                : getAttestedContracts();
    }


    private List<Contract> getAttestedContractsOfChildren() {
        return children.stream()
                .map(child -> child.getAttestedContracts())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }


    /**
     * Every attested contract must have an attestedOn date.
     *
     * @return
     */
    public List<Contract> getAttestedContracts() {
        return getContracts().stream()
                .filter(contract -> contract.getAttestedOn() != null)
                .collect(Collectors.toList());
    }
}
