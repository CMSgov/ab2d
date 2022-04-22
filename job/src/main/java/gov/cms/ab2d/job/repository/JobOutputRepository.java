package gov.cms.ab2d.job.repository;

import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobOutputRepository extends JpaRepository<JobOutput, Long> {

    Optional<JobOutput> findByFilePathAndJob(String filePath, Job job);
}
