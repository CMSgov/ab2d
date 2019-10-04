package gov.cms.ab2d.domain;

import javax.persistence.*;
import java.util.Set;

@Entity
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public Set<Contract> getContracts() {
        return contracts;
    }

    public void setContracts(Set<Contract> contracts) {
        this.contracts = contracts;
    }


}
