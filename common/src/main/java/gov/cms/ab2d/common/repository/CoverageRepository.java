package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.Beneficiary;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Coverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoverageRepository extends JpaRepository<Coverage, Long> {


    @Query(" SELECT c.beneficiary.patientId " +
            "  FROM Coverage c " +
            " WHERE c.contract.id = :contractId " +
            "   AND c.partDMonth = :month ")
    List<String> findActivePatientIds(Long contractId, int month);

    Optional<Coverage> findByContractAndBeneficiaryAndPartDMonth(Contract contract, Beneficiary bene, int month);



    @Modifying
    @Query(" DELETE FROM Coverage c  " +
            " WHERE c.partDMonth = :month  ")
    int deleteByMonth(int month);

    @Modifying
    @Query(" DELETE FROM Coverage c  " +
            " WHERE c.contract.id = :contractId  ")
    int deleteByContractId(Long contractId);


    @Modifying
    @Query(" DELETE FROM Coverage c  " +
            " WHERE c.contract.id = :contractId  " +
            "   AND c.partDMonth = :month  ")
    int deleteByContractIdAndMonth(Long contractId, int month);


}
