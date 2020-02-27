package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {


    Optional<Beneficiary> findByPatientId(String patientId);

}
