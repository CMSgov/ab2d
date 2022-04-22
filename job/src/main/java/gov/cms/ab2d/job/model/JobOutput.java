package gov.cms.ab2d.job.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class JobOutput {

    @Id
    @GeneratedValue
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
    private Boolean downloaded = false;

    @NotNull
    private String checksum;

    @NotNull
    private Long fileLength;
}
