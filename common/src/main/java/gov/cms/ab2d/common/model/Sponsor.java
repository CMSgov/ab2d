package gov.cms.ab2d.common.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

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
}
