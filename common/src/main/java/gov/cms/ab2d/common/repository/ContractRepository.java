package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.contracts.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findContractByContractNumber(String contractNumber);
}
