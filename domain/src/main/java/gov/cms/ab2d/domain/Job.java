package gov.cms.ab2d.domain;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
public class Job {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String jobID;

    @ManyToOne
    private User user;

    @OneToMany(
            mappedBy = "job",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private Set<JobOutput> jobOutput;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String requestURL;
    private JobStatus status;
    private String statusMessage;
    private Integer progress;

    @Pattern(regexp = "^(((Patient)|(ExplanationOfBenefits)),?)*$",
            message = "_type should contain a string of comma-delimited FHIR resource types; "
                    + "currently limited to Patient and ExplanationOfBenefits")
    private String resourceTypes;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job request = (Job) o;
        return Objects.equals(id, request.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
