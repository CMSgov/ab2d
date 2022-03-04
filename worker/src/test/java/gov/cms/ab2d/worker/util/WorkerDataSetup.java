package gov.cms.ab2d.worker.util;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.repository.RoleRepository;
import gov.cms.ab2d.worker.model.ContractWorker;
import gov.cms.ab2d.worker.model.ContractWorkerEntity;
import gov.cms.ab2d.worker.repository.ContractWorkerRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import static java.util.stream.Collectors.toList;

@Component
public class WorkerDataSetup {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ContractWorkerRepository contractRepository;

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

        List<ContractWorkerEntity> contractsToDelete = domainObjects.stream().filter(object -> object instanceof ContractWorkerEntity)
                .map(object -> (ContractWorkerEntity) object).collect(toList());
        contractRepository.deleteAll(contractsToDelete);
        contractRepository.flush();

        domainObjects.clear();
        contractRepository.flush();
    }

    public static final String TEST_PDP_CLIENT = "EileenCFrierson@example.com";

    public static final String VALID_CONTRACT_NUMBER = "ABC123";

    public int countCoverage() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM COVERAGE")) {
            ResultSet rs = statement.executeQuery();

            rs.next();
            return rs.getInt(1);
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }
    public ContractWorker setupWorkerContract(String contractNumber) {
        return setupWorkerContract(contractNumber, OffsetDateTime.now());
    }

    public ContractWorkerEntity setupWorkerContract(String contractNumber, OffsetDateTime attestedOn) {
        ContractWorkerEntity contract = new ContractWorkerEntity();

        contract.setAttestedOn(attestedOn);
        contract.setContractName("Test ContractWorkerDto " + contractNumber);
        contract.setContractNumber(contractNumber);

        contract =  contractRepository.save(contract);
        queueForCleanup(contract);
        return contract;
    }

    public Contract setupContract(String contractNumber) {
        Contract contract = new Contract();
        contract.setContractName("Test ContractWorkerDto " + contractNumber);
        contract.setContractNumber(contractNumber);

        queueForCleanup(contract);
        return contract;
    }


    public void setupContractWithNoAttestation(List<String> clientRoles) {
        setupPdpClient(clientRoles);

        ContractWorkerEntity contract = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        contract.setAttestedOn(null);

        contractRepository.saveAndFlush(contract);
    }

    public void setupContractWithNoAttestation(String clientId, String contractNumber, List<String> clientRoles) {
        setupNonStandardClient(clientId, contractNumber, clientRoles);

        ContractWorkerEntity contract = contractRepository.findContractByContractNumber(contractNumber);
        contract.setAttestedOn(null);

        contractRepository.saveAndFlush(contract);
    }

    private PdpClient savePdpClient(String clientId, Contract contract, List<String> clientRoles) {
        PdpClient pdpClient = new PdpClient();
        pdpClient.setClientId(clientId);
        pdpClient.setOrganization("PDP-" + clientId);
        pdpClient.setContract(contract);
        pdpClient.setEnabled(true);
        pdpClient.setMaxParallelJobs(3);
        for(String clientRole :  clientRoles) {
            // Use existing role or create a new one for the client
            Role role = roleRepository.findRoleByName(clientRole).orElseGet(() -> {
                Role newRole = new Role();
                newRole.setName(clientRole);
                queueForCleanup(clientRole);
                return roleRepository.save(newRole);
            });

            pdpClient.addRole(role);
        }

        pdpClient =  pdpClientRepository.saveAndFlush(pdpClient);
        queueForCleanup(pdpClient);
        return pdpClient;
    }

    public PdpClient setupPdpClient(List<String> clientRoles) {
        PdpClient testPdpClient = pdpClientRepository.findByClientId(TEST_PDP_CLIENT);
        if (testPdpClient != null) {
            return testPdpClient;
        }

        Contract contract = setupContract(VALID_CONTRACT_NUMBER);
//        contractRepository.save(new ContractToContractCoverageMapping().mapWorkerDto(contract));
        queueForCleanup(contract);


        return savePdpClient(TEST_PDP_CLIENT, contract, clientRoles);
    }

    public PdpClient setupNonStandardClient(String clientdId, String contractNumber, List<String> clientRoles) {
        PdpClient testPdpClient = pdpClientRepository.findByClientId(clientdId);
        if (testPdpClient != null) {
            return testPdpClient;
        }

        Contract contract = setupContract(contractNumber);

        return savePdpClient(clientdId, contract, clientRoles);
    }
}
