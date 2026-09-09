package gov.cms.ab2d.contracts.service;

import gov.cms.ab2d.contracts.SpringBootApp;
import gov.cms.ab2d.contracts.repository.ContractRepository;
import gov.cms.ab2d.contracts.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.contracts.hmsapi.HPMSOrganizationInfo;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(locations = "/application.properties")
@Testcontainers
public class AttestationUpdaterServiceTest {

    @Autowired
    private ContractRepository contractRepository;

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Qualifier("for_testing")
    @Autowired
    private AttestationUpdaterServiceImpl aus;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void contractUpdated() {
        assertNotNull(aus);
        aus.pollOrganizations();
        List<Contract> contracts = contractRepository.findAll()
                .stream().filter(contract -> "ABC Org".equals(contract.getHpmsParentOrg()))
                .collect(Collectors.toList());
        assertEquals(1, contracts.size());
    }

    @Test
    public void attestationStatusPopulatedFromHpms() {
        aus.pollOrganizations();

        Contract attested = contractRepository.findContractByContractNumber("S1234").orElseThrow();
        assertEquals(Contract.AttestationStatus.ATTESTED, attested.getAttestationStatus());
        assertNotNull(attested.getAttestedOn());

        Contract notAttested = contractRepository.findContractByContractNumber("S5432").orElseThrow();
        assertEquals(Contract.AttestationStatus.WITHOUT_ATTESTATION, notAttested.getAttestationStatus());
        assertNull(notAttested.getAttestedOn());
    }

    @Test
    public void contractStatusPopulatedFromHpms() {
        aus.pollOrganizations();

        assertEquals("Active",
                contractRepository.findContractByContractNumber("S1234").orElseThrow().getContractStatus());
        assertEquals("Terminated",
                contractRepository.findContractByContractNumber("S4123").orElseThrow().getContractStatus());
    }

    @Test
    public void contractStatusNotBlankedWhenHpmsOmitsIt() {
        aus.pollOrganizations();

        // S5432 comes back from HPMS with no contract status at all
        Contract noStatus = contractRepository.findContractByContractNumber("S5432").orElseThrow();
        assertNull(noStatus.getContractStatus());

        noStatus.setContractStatus("Active");
        contractRepository.save(noStatus);

        aus.pollOrganizations();

        assertEquals("Active",
                contractRepository.findContractByContractNumber("S5432").orElseThrow().getContractStatus());
    }

    @Test
    public void attestationStatusRefreshedOnExistingContract() {
        aus.pollOrganizations();
        Contract stale = contractRepository.findContractByContractNumber("S3412").orElseThrow();
        stale.setAttestationStatus(Contract.AttestationStatus.WITHOUT_ATTESTATION);
        contractRepository.save(stale);

        aus.pollOrganizations();

        Contract refreshed = contractRepository.findContractByContractNumber("S3412").orElseThrow();
        assertEquals(Contract.AttestationStatus.ATTESTED, refreshed.getAttestationStatus());
    }

    @Test
    public void attestationStatusClearedWhenHpmsDropsContract() {
        Contract gone = new Contract("Z9999", "Gone", 42L, "ORG", "Marketing", 1, 1);
        gone.setAttestedOn(OffsetDateTime.now());
        gone.setAttestationStatus(Contract.AttestationStatus.ATTESTED);
        contractRepository.save(gone);

        aus.pollOrganizations();

        Contract cleared = contractRepository.findContractByContractNumber("Z9999").orElseThrow();
        assertEquals(Contract.AttestationStatus.WITHOUT_ATTESTATION, cleared.getAttestationStatus());
        assertNull(cleared.getAttestedOn());
    }

    @Test
    public void noNewContracts() {
        Contract result = aus.addNewContract(null);
        assertNull(result);
    }

    @Test
    void hasChanges() {
        HPMSOrganizationInfo info = new HPMSOrganizationInfo();
        info.setParentOrgId(2);
        Contract contract = new Contract();
        contract.setHpmsParentOrgId(1L);
        assertTrue(contract.hasChanges(info.getContractName(), info.getParentOrgId(), info.getParentOrgName(), info.getOrgMarketingName(), 0, 0));
    }

    @TestConfiguration
    static class MockHpmsFetcherConfig {
        @Mock
        private SQSEventClient logManager;

        @Autowired
        private ContractRepository contractRepository;

        @BeforeEach
        void setUp() {
            MockitoAnnotations.openMocks(this);
        }

        @Qualifier("for_testing")
        @Bean()
        public AttestationUpdaterServiceImpl getMockService() {
            return new AttestationUpdaterServiceImpl(contractRepository, new MockHpmsFetcher(), logManager);
        }
    }
}
