package gov.cms.ab2d.common.util;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Component
public class DataSetup {

    public static final Random RANDOM = new Random();
    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private CoverageRepository coverageRepo;

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

    public CoveragePeriod createCoveragePeriod(Contract contract, int month, int year) {
        CoveragePeriod coveragePeriod = new CoveragePeriod();
        coveragePeriod.setContract(contract);
        coveragePeriod.setMonth(month);
        coveragePeriod.setYear(year);

        return coveragePeriodRepo.saveAndFlush(coveragePeriod);
    }

    public CoverageSearchEvent createCoverageSearchEvent(CoveragePeriod coveragePeriod, String description) {
        CoverageSearchEvent coverageSearchEvent = new CoverageSearchEvent();
        coverageSearchEvent.setCoveragePeriod(coveragePeriod);
        coverageSearchEvent.setNewStatus(JobStatus.SUBMITTED);
        coverageSearchEvent.setDescription(description);

        return coverageSearchEventRepo.saveAndFlush(coverageSearchEvent);
    }

    public Coverage createCoverage(CoveragePeriod coveragePeriod, CoverageSearchEvent coverageSearchEvent, String bene) {
        Coverage coverage = new Coverage();
        coverage.setCoveragePeriod(coveragePeriod);
        coverage.setCoverageSearchEvent(coverageSearchEvent);
        coverage.setBeneficiaryId(bene);
        return coverageRepo.saveAndFlush(coverage);
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

    public Contract setupContract(Sponsor sponsor, String contractNumber) {
        Contract contract = new Contract();
        contract.setAttestedOn(OffsetDateTime.now());
        contract.setContractName("Test Contract");
        contract.setContractNumber(contractNumber);
        contract.setAttestedOn(OffsetDateTime.now());

        contract.setSponsor(sponsor);

        Contract persistedContract = contractRepository.save(contract);

        return persistedContract;
    }

    public void setupContractWithNoAttestation(List<String> userRoles) {
        setupUser(userRoles);

        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
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

    public User setupUser(List<String> userRoles) {
        User testUser = userRepository.findByUsername(TEST_USER);
        if(testUser != null) {
            return testUser;
        }

        Sponsor savedSponsor = createSponsor("Parent Corp.", 456, "Test", 123);

        setupContract(savedSponsor, VALID_CONTRACT_NUMBER);

        return saveUser(savedSponsor, userRoles);
    }
}
