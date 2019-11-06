package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.Attestation;
import gov.cms.ab2d.common.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttestationRepository extends JpaRepository<Attestation, Long> {

    Attestation findOneByContractOrderByAttestedOnDesc(Contract contract);
}
