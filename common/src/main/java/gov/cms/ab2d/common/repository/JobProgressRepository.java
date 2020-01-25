package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.JobProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobProgressRepository extends JpaRepository<JobProgress, Long> {

    // MAY NOT NEED THIS IF I CAN UPDATE USING THE MANAGED ENTITY WHICH IS WITH-IN THE SAME THREAD (SLICE)
    // KEEY THIS COMMENTED HERE, JUST IN CASE I NEED TO REVERT BACK TO THIS LATER.
//    @Modifying
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    @Query(" UPDATE JobProgress j " +
//            "   SET j.progress = :percentageCompleted " +
//            " WHERE j.jobUuid = :jobUuid " +
//            "   AND j.contractId = :contractId " +
//            "   AND j.sliceNumber = :sliceSno ")
//    int updatePercentageCompleted(String jobUuid, long contractId, int sliceNumber, int percentageCompleted);
}
