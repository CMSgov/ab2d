package gov.cms.ab2d.common.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;

import static javax.persistence.GenerationType.SEQUENCE;

@Entity
@Getter
@Setter
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = SEQUENCE, generator = "beneficiary_id_seq")
    @SequenceGenerator(name = "beneficiary_id_seq", sequenceName = "beneficiary_id_seq", allocationSize = 1)
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
