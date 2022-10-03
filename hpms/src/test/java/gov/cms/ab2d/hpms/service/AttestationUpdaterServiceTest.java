package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.hpms.SpringBootTestApp;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizationInfo;
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SpringBootTestApp.class)
@TestPropertySource(locations = "/application.hpms.properties")
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
    public void noNewContracts() {
        List<Contract> result = aus.addNewContracts(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    void hasChanges() {
        HPMSOrganizationInfo info = new HPMSOrganizationInfo();
        info.setParentOrgId(2);
        Contract contract = new Contract();
        contract.setHpmsParentOrgId(1L);
        assertTrue(info.hasChanges(contract));
    }

    @Test
    void updateCoverage() {
        HPMSOrganizationInfo info = new HPMSOrganizationInfo();
        info.setParentOrgId(2);
        Contract contract = new Contract();
        contract.setHpmsParentOrgId(1L);
        info.updateContract(contract);
        assertEquals(2L, (long) contract.getHpmsParentOrgId());
    }

    @TestConfiguration
    static class MockHpmsFetcherConfig {
        @Mock
        private LogManager logManager;

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
