package gov.cms.ab2d.domain;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter
public class Job {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String jobID;

    @ManyToOne
    private User user;

    @OneToMany(
            mappedBy = "request",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private Set<JobOutput> jobOutput;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String requestURL;
    private JobStatus status;
    private String statusMessage;
    private Integer progress;
    private String resourceTypes;

}
