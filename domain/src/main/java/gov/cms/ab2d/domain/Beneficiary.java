package gov.cms.ab2d.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;

@Entity
@Getter
@Setter
public class Beneficiary {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String patientId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "coverage",
            joinColumns = @JoinColumn(name = "beneficiary_id"),
            inverseJoinColumns = @JoinColumn(name = "contract_id"))
    private Set<Contract> contracts;

}
