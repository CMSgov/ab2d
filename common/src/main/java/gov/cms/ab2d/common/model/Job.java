package gov.cms.ab2d.common.model;

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
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.GenerationType.SEQUENCE;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Job {

    @Id
    @GeneratedValue(strategy = SEQUENCE, generator = "job_id_seq")
    @SequenceGenerator(name = "job_id_seq", sequenceName = "job_id_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true)
    private String jobId;

    @ManyToOne
    @JoinColumn(name = "user_account_id")
    private User user;

    @OneToMany(
            mappedBy = "job",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<JobOutput> jobOutput = new ArrayList<>();

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime completedAt;

    private String requestUrl;
    private JobStatus status;
    private String statusMessage;
    private Integer progress;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastPollTime;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime expiresAt;

    @Pattern(regexp = "ExplanationOfBenefits", message = "_type should be ExplanationOfBenefits")
    private String resourceTypes; // for now just limited to ExplanationOfBenefits
}
