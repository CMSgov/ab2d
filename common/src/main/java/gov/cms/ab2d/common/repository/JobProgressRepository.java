package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.JobProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface JobProgressRepository extends JpaRepository<JobProgress, Long> {

    @Query("FROM JobProgress jp WHERE jp.job.id = :jobId AND jp.contract.id = :contractId AND jp.sliceNumber = :sliceNo")
    JobProgress findOne(Long jobId, Long contractId, Integer sliceNo);
}
