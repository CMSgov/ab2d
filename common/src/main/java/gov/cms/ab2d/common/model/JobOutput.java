package gov.cms.ab2d.common.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

import static javax.persistence.GenerationType.SEQUENCE;

@Entity
@Getter
@Setter
public class JobOutput {

    @Id
    @GeneratedValue(strategy = SEQUENCE, generator = "job_output_id_seq")
    @SequenceGenerator(name = "job_output_id_seq", sequenceName = "job_output_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(columnDefinition = "text")
    private String filePath;

    private String fhirResourceType;

    private boolean error;
}
