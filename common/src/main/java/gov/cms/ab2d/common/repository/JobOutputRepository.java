package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobOutputRepository extends JpaRepository<JobOutput, Long> {

    Optional<JobOutput> findByFilePathAndJob(String filePath, Job job);

    //HQL doesn't seem to have a way to modify dates. It was with a native query or adding a special  query to find the DB time
    @Query(nativeQuery = true, value = "select jobOutput.id as id, jobOutput.checksum as checksum, jobOutput.downloaded as downloaded, " +
            "                   jobOutput.error as error, jobOutput.fhir_resource_type as fhir_resource_type, " +
            "                   jobOutput.file_length as file_length, jobOutput.file_path as file_path, " +
            "                   jobOutput.job_id as job_id, jobOutput.last_download_at as last_download_at " +
            " from job_output jobOutput " +
            "    cross join job job1_ " +
            "        where jobOutput.job_id=job1_.id " +
            "          and jobOutput.last_download_at NOT BETWEEN CURRENT_TIMESTAMP - (INTERVAL '1 min' * :interval) and CURRENT_TIMESTAMP " +
            "          and job1_.expires_at>=CURRENT_TIMESTAMP")
    Optional<List<JobOutput>> findByDownloadExpiredAndJobExpired(int interval);
}
