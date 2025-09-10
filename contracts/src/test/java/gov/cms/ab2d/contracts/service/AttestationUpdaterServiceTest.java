package gov.cms.ab2d.contracts.service;

import gov.cms.ab2d.contracts.SpringBootApp;
import gov.cms.ab2d.contracts.repository.ContractRepository;
import gov.cms.ab2d.contracts.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.contracts.hmsapi.HPMSOrganizationInfo;
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
