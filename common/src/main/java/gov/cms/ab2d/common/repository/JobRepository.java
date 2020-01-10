package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    @Modifying
    @Query("update Job j set j.status = gov.cms.ab2d.common.model.JobStatus.CANCELLED where j.jobUuid = :jobUuid")
    void cancelJobByJobUuid(@Param("jobUuid") String jobUuid);

    Job findByJobUuid(String jobUuid);

    @Query("select j from Job j where j.user = :user and (j.status = 'IN_PROGRESS' or j.status = 'SUBMITTED')")
    List<Job> findActiveJobsByUser(User user);

    @Query("select j from Job j where j.user = :user and j.contract = :contract and (j.status = 'IN_PROGRESS' or j.status = 'SUBMITTED')")
    List<Job> findActiveJobsByUserAndContract(User user, Contract contract);

    @Query("SELECT j.status FROM Job j WHERE j.jobUuid = :jobUuid ")
    JobStatus findJobStatus(String jobUuid);

    @Query("FROM Job j WHERE j.createdAt < :createdAt AND j.status = 'IN_PROGRESS' AND j.completedAt IS NULL ")
    List<Job> findStuckJobs(OffsetDateTime createdAt);

    @Modifying
    @Query("UPDATE Job j SET j.status = 'SUBMITTED' WHERE j.jobUuid IN :jobUuids ")
    void resetJobsToSubmittedStatus(List<String> jobUuids);

}
