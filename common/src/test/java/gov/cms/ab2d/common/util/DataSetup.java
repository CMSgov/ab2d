package gov.cms.ab2d.common.util;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class DataSetup {

    @Autowired
    ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SponsorRepository sponsorRepository;

    public static final String TEST_USER = "EileenCFrierson@example.com";

    public Contract createContract() {
        Contract contract = new Contract();
        contract.setAttestedOn(OffsetDateTime.now());
        contract.setContractName("Test Contract");
        contract.setContractNumber("ABC123");

        Sponsor sponsor = createSponsor();
        contract.setSponsor(sponsor);

        Contract persistedContract = contractRepository.saveAndFlush(contract);

        return persistedContract;
    }

    public Sponsor createSponsor() {
        Sponsor parent = new Sponsor();
        parent.setOrgName("Parent Corp.");
        parent.setHpmsId(456);
        parent.setLegalName("Parent Corp.");

        Sponsor sponsor = new Sponsor();
        sponsor.setOrgName("Test");
        sponsor.setHpmsId(123);
        sponsor.setLegalName("Test");
        sponsor.setParent(parent);
        return sponsorRepository.save(sponsor);
    }

    public void setupUser(List<String> userRoles) {
        User testUser = userRepository.findByUsername(TEST_USER);
        if(testUser != null) {
            return;
        }

        Sponsor savedSponsor = createSponsor();

        User user = new User();
        user.setEmail(TEST_USER);
        user.setFirstName("Eileen");
        user.setLastName("Frierson");
        user.setUsername(TEST_USER);
        user.setSponsor(savedSponsor);
        user.setEnabled(true);
        for(String userRole :  userRoles) {
            Role role = new Role();
            role.setName(userRole);
            user.addRole(role);
        }
        userRepository.save(user);
    }
}
