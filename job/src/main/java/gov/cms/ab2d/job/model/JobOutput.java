package gov.cms.ab2d.job.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class JobOutput {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id")
    @NotNull
    private Job job;

    @Column(columnDefinition = "text")
    @NotNull
    private String filePath;

    private String fhirResourceType;

    @NotNull
    private Boolean error;

    @NotNull
    private int downloaded = 0;

    @NotNull
    private String checksum;

    @NotNull
    private Long fileLength;

    private OffsetDateTime lastDownloadAt;
}
