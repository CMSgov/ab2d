package gov.cms.ab2d.common.util;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

import static java.util.stream.Collectors.toList;

@Component
public class DataSetup {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private CoverageSearchRepository coverageSearchRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    CoverageDeltaTestRepository coverageDeltaTestRepository;

    private final Set<Object> domainObjects = new HashSet<>();

    public void queueForCleanup(Object object) {
        domainObjects.add(object);
    }

    public void cleanup() {

        // All of the coverage metadata tests assume that you completely
        // wipe the tables between tests and that the tables started as empty tables.
        // Based on these assumptions it is safe to simply delete everything associated
        // with those tables
        coverageDeltaTestRepository.deleteAll();
        deleteCoverage();
        coverageSearchEventRepo.deleteAll();
        coverageSearchRepo.deleteAll();
        coveragePeriodRepo.deleteAll();

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
    }

    public static final String TEST_PDP_CLIENT = "EileenCFrierson@example.com";

    public static final String BAD_CONTRACT_NUMBER = "WrongContract";

    public static final String VALID_CONTRACT_NUMBER = "ABC123";

    public static String createBeneId() {
        return "patientId_" + Instant.now().getNano();
    }

    public static String createMbiId() {
        return "mbi_" + Instant.now().getNano();
    }

    public CoveragePeriod createCoveragePeriod(Contract contract, int month, int year) {
        CoveragePeriod coveragePeriod = new CoveragePeriod();
        coveragePeriod.setContract(contract);
        coveragePeriod.setMonth(month);
        coveragePeriod.setYear(year);

        return coveragePeriodRepo.saveAndFlush(coveragePeriod);
    }

    public void deleteCoveragePeriod(CoveragePeriod coveragePeriod) {
        coveragePeriodRepo.delete(coveragePeriod);
    }

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

    public void deleteCoverage() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM coverage")) {
            statement.execute();
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    public List<Coverage> findCoverage() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM coverage")) {
            ResultSet rs = statement.executeQuery();

            List<Coverage> memberships = new ArrayList<>();
            while (rs.next()) {
                int coveragePeriod = rs.getInt(1);
                long searchEventId = rs.getInt(2);
                String contract = rs.getString(3);
                int year = rs.getInt(4);
                int month = rs.getInt(5);
                long beneficiaryId = rs.getLong(6);
                String currentMbi = rs.getString(7);
                String historicalMbis = rs.getString(8);

                memberships.add(new Coverage(coveragePeriod, searchEventId, contract, year, month, beneficiaryId, currentMbi, historicalMbis));
            }

            return memberships;
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    public CoverageSearchEvent createCoverageSearchEvent(CoveragePeriod coveragePeriod, String description) {
        CoverageSearchEvent coverageSearchEvent = new CoverageSearchEvent();
        coverageSearchEvent.setCoveragePeriod(coveragePeriod);
        coverageSearchEvent.setNewStatus(JobStatus.SUBMITTED);
        coverageSearchEvent.setDescription(description);

        return coverageSearchEventRepo.saveAndFlush(coverageSearchEvent);
    }

    public void deleteCoverageSearchEvent(CoverageSearchEvent event) {
        coverageSearchEventRepo.delete(event);
    }

    public Contract setupContract(String contractNumber) {
        return setupContract(contractNumber, OffsetDateTime.now());
    }

    public Contract setupContract(String contractNumber, OffsetDateTime attestedOn) {
        Contract contract = new Contract();

        contract.setAttestedOn(attestedOn);
        contract.setContractName("Test Contract " + contractNumber);
        contract.setContractNumber(contractNumber);

        contract =  contractRepository.save(contract);
        queueForCleanup(contract);
        return contract;
    }

    public void deleteContract(Contract contract) {
        contractRepository.delete(contract);
    }

    public void setupContractWithNoAttestation(List<String> clientRoles) {
        setupPdpClient(clientRoles);

        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        Contract contract = contractOptional.get();
        contract.setAttestedOn(null);

        contractRepository.saveAndFlush(contract);
    }

    public void setupContractWithNoAttestation(String clientId, String contractNumber, List<String> clientRoles) {
        setupNonStandardClient(clientId, contractNumber, clientRoles);

        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(contractNumber);
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        Contract contract = contractOptional.get();
        contract.setAttestedOn(null);

        contractRepository.saveAndFlush(contract);
    }

    public void setupContractSponsorForParentClientData(List<String> clientRoles) {
        Contract contract = setupContract("ABC123");

        savePdpClient(TEST_PDP_CLIENT, contract, clientRoles);
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
                return roleRepository.save(newRole);
            });

            pdpClient.addRole(role);
            queueForCleanup(role);
        }

        pdpClient =  pdpClientRepository.save(pdpClient);
        queueForCleanup(pdpClient);
        return pdpClient;
    }

    public void deletePdpClient(PdpClient pdpClient) {
        Set<Role> roles = pdpClient.getRoles();
        roles.forEach(c -> roleRepository.delete(c));
        pdpClientRepository.delete(pdpClient);
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

    public void createRole(String sponsorRole) {
        Role role = new Role();
        role.setName(sponsorRole);
        roleRepository.save(role);
        queueForCleanup(role);
    }
}
