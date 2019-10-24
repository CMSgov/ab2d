package gov.cms.ab2d.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(
            mappedBy = "job",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<JobOutput> jobOutput = new ArrayList<>();
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;
    private String requestURL;
    private JobStatus status;
    private String statusMessage;
    private Integer progress;
    private OffsetDateTime lastPollTime;
    private OffsetDateTime expires;

    @Pattern(regexp = "ExplanationOfBenefits", message = "_type should be ExplanationOfBenefits")
    private String resourceTypes; // for now just limited to ExplanationOfBenefits
}
