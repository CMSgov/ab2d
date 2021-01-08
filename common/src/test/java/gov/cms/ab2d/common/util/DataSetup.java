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
    private UserRepository userRepository;

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

    private final Set<Object> domainObjects = new HashSet<>();

    public void queueForCleanup(Object object) {
        domainObjects.add(object);
    }

    public void cleanup() {

        // All of the coverage metadata tests assume that you completely
        // wipe the tables between tests and that the tables started as empty tables.
        // Based on these assumptions it is safe to simply delete everything associated
        // with those tables
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

        List<User> usersToDelete = domainObjects.stream().filter(object -> object instanceof User)
                .map(object -> (User) object).collect(toList());
        usersToDelete.forEach(user -> {
            user = userRepository.findByUsername(user.getUsername());

            if (user != null) {
                userRepository.delete(user);
                userRepository.flush();
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

    public static final String TEST_USER = "EileenCFrierson@example.com";

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
                long id = rs.getLong(1);
                int periodId = rs.getInt(2);
                long searchEventId = rs.getInt(3);
                String beneficiaryId = rs.getString(4);
                String currentMbi = rs.getString(5);
                String historicalMbis = rs.getString(6);

                memberships.add(new Coverage(id, periodId, searchEventId, beneficiaryId, currentMbi, historicalMbis));
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
        Contract contract = new Contract();
        contract.setAttestedOn(OffsetDateTime.now());
        contract.setContractName("Test Contract " + contractNumber);
        contract.setContractNumber(contractNumber);

        contract =  contractRepository.save(contract);
        queueForCleanup(contract);
        return contract;
    }

    public void deleteContract(Contract contract) {
        contractRepository.delete(contract);
    }

    public void setupContractWithNoAttestation(List<String> userRoles) {
        setupUser(userRoles);

        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        Contract contract = contractOptional.get();
        contract.setAttestedOn(null);

        contractRepository.saveAndFlush(contract);
    }

    public void setupContractWithNoAttestation(String username, String contractNumber, List<String> userRoles) {
        setupNonStandardUser(username, contractNumber, userRoles);

        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(contractNumber);
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        Contract contract = contractOptional.get();
        contract.setAttestedOn(null);

        contractRepository.saveAndFlush(contract);
    }

    public void setupContractSponsorForParentUserData(List<String> userRoles) {
        Contract contract = setupContract("ABC123");

        saveUser(TEST_USER, contract, userRoles);
    }

    public void setupUserBadSponsorData(List<String> userRoles) {
        setupContract("ABC123");

        Contract contract = setupContract(BAD_CONTRACT_NUMBER);

        saveUser(TEST_USER, contract, userRoles);
    }

    private User saveUser(String username, Contract contract, List<String> userRoles) {
        User user = new User();
        user.setEmail(username);
        user.setFirstName(username);
        user.setLastName(username);
        user.setUsername(username);
        user.setContract(contract);
        user.setEnabled(true);
        user.setMaxParallelJobs(3);
        for(String userRole :  userRoles) {
            Role role = new Role();
            role.setName(userRole);
            roleRepository.save(role);
            user.addRole(role);
            queueForCleanup(role);
        }

        user =  userRepository.save(user);
        queueForCleanup(user);
        return user;
    }

    public void deleteUser(User user) {
        Set<Role> roles = user.getRoles();
        roles.forEach(c -> roleRepository.delete(c));
        userRepository.delete(user);
    }

    public User setupUser(List<String> userRoles) {
        User testUser = userRepository.findByUsername(TEST_USER);
        if (testUser != null) {
            return testUser;
        }

        Contract contract = setupContract(VALID_CONTRACT_NUMBER);

        return saveUser(TEST_USER, contract, userRoles);
    }

    public User setupNonStandardUser(String username, String contractNumber, List<String> userRoles) {
        User testUser = userRepository.findByUsername(username);
        if (testUser != null) {
            return testUser;
        }

        Contract contract = setupContract(contractNumber);

        return saveUser(username, contract, userRoles);
    }
}
