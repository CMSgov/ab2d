package gov.cms.ab2d.worker.util;

import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.repository.RoleRepository;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import static java.util.stream.Collectors.toList;

@Component
public class WorkerDataSetup {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private RoleRepository roleRepository;

    private final Set<Object> domainObjects = new HashSet<>();

    public void queueForCleanup(Object object) {
        domainObjects.add(object);
    }

    public void cleanup() {

        List<Job> jobsToDelete = domainObjects.stream().filter(object -> object instanceof Job)
                .map(object -> (Job) object).collect(toList());
        jobsToDelete.forEach(job -> {
            job = jobRepository.findByJobUuid(job.getJobUuid());
            jobRepository.delete(job);
            jobRepository.flush();
        });

        List<PdpClient> clientsToDelete = domainObjects.stream().filter(object -> object instanceof PdpClient)
                .map(object -> (PdpClient) object).collect(toList());
        clientsToDelete.forEach(pdpClient -> {
            pdpClient = pdpClientRepository.findByClientId(pdpClient.getClientId());

            if (pdpClient != null) {
                pdpClientRepository.delete(pdpClient);
                pdpClientRepository.flush();
            }
        });

        List<Role> rolesToDelete = domainObjects.stream().filter(object -> object instanceof Role)
                .map(object -> (Role) object).collect(toList());
        rolesToDelete.forEach(role -> {
            Optional<Role> roleOptional = roleRepository.findRoleByName(role.getName());

            if (roleOptional.isPresent()) {
                roleRepository.delete(roleOptional.get());
                roleRepository.flush();
            }
        });

        List<Contract> contractsToDelete = domainObjects.stream().filter(object -> object instanceof Contract)
                .map(object -> (Contract) object).collect(toList());
        contractRepository.deleteAll(contractsToDelete);
        contractRepository.flush();

        domainObjects.clear();
        contractRepository.flush();
    }

    public ContractDTO setupWorkerContract(String contractNumber, OffsetDateTime attestedOn) {
        ContractDTO contract = new ContractDTO(null, contractNumber, "Test ContractWorkerDto " + contractNumber, attestedOn, Contract.ContractType.NORMAL);

        queueForCleanup(contract);
        return contract;
    }

    public Contract setupContract(String contractNumber, OffsetDateTime attestedOn) {
        Contract contract = new Contract();
        contract.setContractName("Test ContractWorkerDto " + contractNumber);
        contract.setContractNumber(contractNumber);
        contract.setAttestedOn(attestedOn);
        queueForCleanup(contract);
        return contract;
    }
}
