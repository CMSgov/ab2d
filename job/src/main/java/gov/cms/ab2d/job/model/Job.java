package gov.cms.ab2d.job.model;

import gov.cms.ab2d.common.model.SinceSource;
import gov.cms.ab2d.common.model.TooFrequentInvocations;
import gov.cms.ab2d.eventclient.events.FileEvent;
import gov.cms.ab2d.eventclient.events.JobStatusChangeEvent;
import gov.cms.ab2d.fhir.FhirVersion;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static gov.cms.ab2d.job.model.JobStatus.CANCELLED;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static jakarta.persistence.EnumType.STRING;

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
    @NotNull
    private String jobUuid;

    @NotNull
    private String organization;

    @OneToMany(
            mappedBy = "job",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<JobOutput> jobOutputs = new ArrayList<>();

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @NotNull
    private OffsetDateTime createdAt;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime completedAt;

    private String requestUrl;

    @Enumerated(STRING)
    @NotNull
    private JobStatus status;
    private String statusMessage;
    private String outputFormat;
    private Integer progress;

    @Enumerated(STRING)
    private FhirVersion fhirVersion = STU3;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastPollTime;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime since;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime expiresAt;

    @Pattern(regexp = EOB, message = "_type should be ExplanationOfBenefit")
    private String resourceTypes; // for now just limited to ExplanationOfBenefit

    // Default a job to started by a PDP and only override if necessary
    @Enumerated(STRING)
    @NotNull
    private JobStartedBy startedBy = JobStartedBy.PDP;

    @Enumerated(STRING)
    private SinceSource sinceSource;

    @NotNull
    private String contractNumber;

    public void addJobOutput(JobOutput jobOutput) {
        jobOutputs.add(jobOutput);
        jobOutput.setJob(this);
    }

    public boolean hasJobBeenCancelled() {
        return CANCELLED == status;
    }

    public void pollAndUpdateTime(int delaySeconds) {
        if (pollingTooMuch(delaySeconds)) {
            throw new TooFrequentInvocations("polling too frequently");
        }
        lastPollTime = OffsetDateTime.now();
    }

    public boolean isExpired(int ttlHours) {
        return status.isExpired(completedAt, ttlHours);
    }

    private boolean pollingTooMuch(int delaySeconds) {
        return lastPollTime != null && lastPollTime.plusSeconds(delaySeconds).isAfter(OffsetDateTime.now());
    }

    public JobStatusChangeEvent buildJobStatusChangeEvent(JobStatus newStatus, String statusMessage) {
        final String oldStatus = (status == null) ? null : status.name();
        return new JobStatusChangeEvent(organization, jobUuid, oldStatus, newStatus.name(), statusMessage);
    }

    public FileEvent buildFileEvent(File file, FileEvent.FileStatus status) {
        return new FileEvent(organization, jobUuid, file, status);
    }
}
