package gov.cms.ab2d.job.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class JobOutput {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "job_output_seq")
    @SequenceGenerator(name = "job_output_seq", sequenceName = "public.job_output_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
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
