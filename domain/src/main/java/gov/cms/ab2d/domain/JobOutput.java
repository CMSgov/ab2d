package gov.cms.ab2d.domain;

import javax.persistence.*;

@Entity
public class JobOutput {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Job job;

    @Column(columnDefinition = "text")
    private String filePath;

    private String fhirResourceType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFhirResourceType() {
        return fhirResourceType;
    }

    public void setFhirResourceType(String fhirResourceType) {
        this.fhirResourceType = fhirResourceType;
    }


}
