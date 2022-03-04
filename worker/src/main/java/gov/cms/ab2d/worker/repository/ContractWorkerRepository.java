package gov.cms.ab2d.worker.repository;

import gov.cms.ab2d.worker.model.ContractWorkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractWorkerRepository extends JpaRepository<ContractWorkerEntity, Long> {
    ContractWorkerEntity findContractByContractNumber(String contractNumber);
}
