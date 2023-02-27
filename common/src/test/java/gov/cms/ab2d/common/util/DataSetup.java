package gov.cms.ab2d.common.util;

import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.repository.RoleRepository;
import gov.cms.ab2d.common.service.ContractServiceStub;
import gov.cms.ab2d.contracts.model.Contract;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.toList;

@Component
@Import(ContractServiceTestConfig.class)
public class DataSetup {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ContractServiceStub contractService;

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    private RoleRepository roleRepository;

    private final Set<Object> domainObjects = new HashSet<>();

    Random randomGenerator = new Random();

    public void queueForCleanup(Object object) {
        domainObjects.add(object);
    }

    public void cleanup() {

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

        contractService.reset();

        domainObjects.clear();
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
    public Contract setupContract(String contractNumber) {
        return setupContract(contractNumber, OffsetDateTime.now());
    }

    public Contract setupContract(String contractNumber, OffsetDateTime attestedOn) {
        Contract contract = new Contract();

        // prevent errors if two tests try to add the same contract
        contract.setAttestedOn(attestedOn);
        contract.setContractName("Test Contract " + contractNumber);
        contract.setContractNumber(contractNumber);
        contract.setId(randomGenerator.nextLong(200L, 400L));

        contractService.updateContract(contract);
        queueForCleanup(contract);
        return contract;
    }

    public void setupContractWithNoAttestation(List<String> clientRoles) {
        setupPdpClient(clientRoles);

        Optional<Contract> contractOptional = contractService.getContractByContractNumber(VALID_CONTRACT_NUMBER);
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        Contract contract = contractOptional.get();
        contract.setAttestedOn(null);

        contractService.updateContract(contract);
    }

    public void setupContractSponsorForParentClientData(List<String> clientRoles) {
        Contract contract = setupContract("ABC123");

        savePdpClient(TEST_PDP_CLIENT, contract, clientRoles);
    }

    private PdpClient savePdpClient(String clientId, Contract contract, List<String> clientRoles) {
        PdpClient pdpClient = new PdpClient();
        pdpClient.setClientId(clientId);
        pdpClient.setOrganization("PDP-" + clientId);
        pdpClient.setContractId(contract.getId());
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

        pdpClient =  pdpClientRepository.save(pdpClient);
        queueForCleanup(pdpClient);
        return pdpClient;
    }

    public PdpClient setupPdpClient(List<String> clientRoles) {
        PdpClient testPdpClient = pdpClientRepository.findByClientId(TEST_PDP_CLIENT);
        if (testPdpClient != null) {
            return testPdpClient;
        }

        Contract contract = setupContract(VALID_CONTRACT_NUMBER);

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
