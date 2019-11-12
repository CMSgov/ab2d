package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    @Modifying
    @Query("update Job j set j.status = gov.cms.ab2d.common.model.JobStatus.CANCELLED where j.jobUuid = :jobUuid")
    void cancelJobByJobUuid(@Param("jobUuid") String jobUuid);

    Job findByJobUuid(String jobUuid);
}
