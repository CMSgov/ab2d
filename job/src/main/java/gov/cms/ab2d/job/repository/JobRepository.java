package gov.cms.ab2d.job.repository;

import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStartedBy;
import gov.cms.ab2d.job.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("update Job j set j.status = gov.cms.ab2d.job.model.JobStatus.CANCELLED where j.jobUuid = :jobUuid")
    void cancelJobByJobUuid(@Param("jobUuid") String jobUuid);

    Job findByJobUuid(String jobUuid);

    @Query("select j from Job j where j.organization = :organization and (j.status = 'IN_PROGRESS' or j.status = 'SUBMITTED')")
    List<Job> findActiveJobsByClient(String organization);

    List<Job> findByContractNumberEqualsAndStatusInAndStartedByOrderByCompletedAtDesc(
            String contractNumber, List<JobStatus> statuses, JobStartedBy startedBy);

    @Query("FROM Job j WHERE j.createdAt < :createdAt AND j.status = 'IN_PROGRESS' AND j.completedAt IS NULL ")
    List<Job> findStuckJobs(OffsetDateTime createdAt);

    @Modifying
    @Query("UPDATE Job j SET j.status = 'SUBMITTED' WHERE j.jobUuid IN :jobUuids ")
    void resetJobsToSubmittedStatus(List<String> jobUuids);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE Job j SET j.progress = :percentageCompleted WHERE j.jobUuid = :jobUuid ")
    void updatePercentageCompleted(String jobUuid, int percentageCompleted);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.contractNumber = :contractNumber AND j.status IN :statuses")
    int countJobByContractNumberAndStatus(String contractNumber, List<JobStatus> statuses);

    List<Job> findByJobUuidIn(List<String> jobUuids);
}
