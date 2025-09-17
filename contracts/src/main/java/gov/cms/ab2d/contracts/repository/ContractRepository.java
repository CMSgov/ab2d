package gov.cms.ab2d.contracts.repository;


import gov.cms.ab2d.contracts.model.Contract;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findContractByContractNumber(String contractNumber);
}
