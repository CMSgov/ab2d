package gov.cms.ab2d.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.EnumType.STRING;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Configurable(autowire = Autowire.BY_TYPE)
public class Job {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true)
    private String jobUuid;

    @ManyToOne
    @JoinColumn(name = "user_account_id")
    private User user;

    @OneToMany(
            mappedBy = "job",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<JobOutput> jobOutputs = new ArrayList<>();

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime completedAt;

    private String requestUrl;

    @Enumerated(STRING)
    private JobStatus status;
    private String statusMessage;
    private Integer progress;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastPollTime;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime expiresAt;

    @Pattern(regexp = "ExplanationOfBenefits", message = "_type should be ExplanationOfBenefits")
    private String resourceTypes; // for now just limited to ExplanationOfBenefits

    @OneToOne()
    @JoinColumn(name = "id")
    @Nullable
    private Contract contract;

    public void addJobOutput(JobOutput jobOutput) {
        jobOutputs.add(jobOutput);
        jobOutput.setJob(this);
    }
}
