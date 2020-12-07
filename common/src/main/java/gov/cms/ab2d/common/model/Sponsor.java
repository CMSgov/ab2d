package gov.cms.ab2d.common.model;

import lombok.*;

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

import static java.util.stream.Collectors.toList;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Sponsor extends TimestampBase {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    @EqualsAndHashCode.Include
    private Integer hpmsId;

    @NotNull
    @EqualsAndHashCode.Include
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

    @OneToMany(cascade = CascadeType.ALL,
            mappedBy = "sponsorIPID.sponsor",
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private Set<SponsorIP> sponsorIPs = new HashSet<>();

    public Sponsor(@NotNull String orgName, String legalName) {
        this.hpmsId = Integer.MAX_VALUE;    // We only have the parent id in the hpms feed
        this.orgName = orgName;
        this.legalName = legalName;
    }

    public boolean hasContract(String contractNum) {
        return contracts.stream().anyMatch(contract -> contractNum.equalsIgnoreCase(contract.getContractNumber()));
    }

    public List<Contract> getAggregatedAttestedContracts() {
        return parent == null
                ? getAttestedContractsOfChildren()
                : getAttestedContracts();
    }

    private List<Contract> getAttestedContractsOfChildren() {
        return children.stream()
                .map(Sponsor::getAttestedContracts)
                .flatMap(Collection::stream)
                .collect(toList());
    }

    /**
     * Every attested contract must have an attestedOn date.
     *
     * @return List of contracts
     */
    public List<Contract> getAttestedContracts() {
        return getContracts().stream()
                .filter(Contract::hasAttestation)
                .collect(toList());
    }
}
