package gov.cms.ab2d.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class JobProgress {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id")
    @EqualsAndHashCode.Include
    private Job job;

    @ManyToOne
    @JoinColumn(name = "contract_id")
    @EqualsAndHashCode.Include
    private Contract contract;

    @EqualsAndHashCode.Include
    private Integer sliceNumber;


    private Integer progress;
}
