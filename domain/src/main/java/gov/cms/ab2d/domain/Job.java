package gov.cms.ab2d.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Job {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
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
    private List<JobOutput> jobOutput;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String requestURL;
    private JobStatus status;
    private String statusMessage;
    private Integer progress;
    private LocalDateTime lastPollTime;
    private LocalDateTime expires;

    @Pattern(regexp = "ExplanationOfBenefits", message = "_type should be ExplanationOfBenefits")
    private String resourceTypes; // for now just limited to ExplanationOfBenefits
}
