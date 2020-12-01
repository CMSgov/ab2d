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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class DataSetup {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

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


    public Sponsor createSponsor(String parentName, int parentHpmsId, String childName, int childHpmsId) {
        Sponsor parent = new Sponsor();
        parent.setOrgName(parentName);
        parent.setHpmsId(parentHpmsId);
        parent.setLegalName(parentName);

        Sponsor sponsor = new Sponsor();
        sponsor.setOrgName(childName);
        sponsor.setHpmsId(childHpmsId);
        sponsor.setLegalName(childName);
        sponsor.setParent(parent);
        return sponsorRepository.save(sponsor);
    }

    public void deleteSponsor(Sponsor sponsor) {
        sponsorRepository.delete(sponsor);
        if (sponsor.getParent() != null) {
            deleteSponsor(sponsor.getParent());
        }
    }

    public Contract setupContract(Sponsor sponsor, String contractNumber) {
        Contract contract = new Contract();
        contract.setAttestedOn(OffsetDateTime.now());
        contract.setContractName("Test Contract");
        contract.setContractNumber(contractNumber);

        contract.setSponsor(sponsor);

        return contractRepository.save(contract);
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

    public void setupContractSponsorForParentUserData(List<String> userRoles) {
        Sponsor savedSponsor = createSponsor("Parent Corp.", 456, "Test", 123);

        setupContract(savedSponsor, "ABC123");

        saveUser(savedSponsor.getParent(), userRoles);
    }

    public void setupUserBadSponsorData(List<String> userRoles) {
        Sponsor savedSponsor = createSponsor("Parent Corp.", 456, "Test", 123);

        setupContract(savedSponsor, "ABC123");

        Sponsor savedBadSponsor = createSponsor("Bad Parent Corp.", 789, "Bad Child", 10001);

        setupContract(savedBadSponsor, BAD_CONTRACT_NUMBER);

        saveUser(savedSponsor, userRoles);
    }

    private User saveUser(Sponsor sponsor, List<String> userRoles) {
        User user = new User();
        user.setEmail(TEST_USER);
        user.setFirstName("Eileen");
        user.setLastName("Frierson");
        user.setUsername(TEST_USER);
        user.setSponsor(sponsor);
        user.setEnabled(true);
        user.setMaxParallelJobs(3);
        for(String userRole :  userRoles) {
            Role role = new Role();
            role.setName(userRole);
            roleRepository.save(role);
            user.addRole(role);
        }

        return userRepository.save(user);
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

        Sponsor savedSponsor = createSponsor("Parent Corp.", 456, "Test", 123);

        setupContract(savedSponsor, VALID_CONTRACT_NUMBER);

        return saveUser(savedSponsor, userRoles);
    }
}
