package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.hpms.SpringBootTestApp;
import gov.cms.ab2d.hpms.hmsapi.HPMSAttestation;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizationInfo;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        List<Contract> result = aus.addNewContracts(Lists.emptyList());
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

    @Test
    void considerContract() throws NoSuchMethodException {
        Method method = aus.getClass().getDeclaredMethod("considerContract", List.class, Contract.class, HPMSOrganizationInfo.class);
        method.setAccessible(true);
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setAttestedOn(OffsetDateTime.now());
        List<Contract> list = new ArrayList<>();
        Assertions.assertThrows(Exception.class,
                () -> method.invoke(aus, list, contract, null));
    }

    @Test
    void considerContractNoAttestation() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = aus.getClass().getDeclaredMethod("considerContract", List.class, Contract.class, HPMSOrganizationInfo.class);
        method.setAccessible(true);
        Contract contract = new Contract();
        contract.setId(1L);
        List<Contract> list = new ArrayList<>();
        method.invoke(aus, list, contract, null);
        assertTrue(list.isEmpty());
    }

    @Test
    void considerContractAdd() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = aus.getClass().getDeclaredMethod("considerContract", List.class, Contract.class, HPMSOrganizationInfo.class);
        method.setAccessible(true);
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setAttestedOn(OffsetDateTime.now());
        List<Contract> list = new ArrayList<>();
        method.invoke(aus, list, contract, new HPMSOrganizationInfo());
        assertTrue(list.size() > 0);
    }

    @Test
    void updateContractIfChanged() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        AttestationUpdaterServiceImpl service = new AttestationUpdaterServiceImpl(null, null, new LogManager(null, null, null));
        Method method = service.getClass().getDeclaredMethod("updateContractIfChanged", HPMSAttestation.class, Contract.class);
        method.setAccessible(true);
        Contract contract = new Contract();
        contract.setAttestedOn(OffsetDateTime.now());
        contract.setId(1L);
        Assertions.assertThrows(Exception.class,
                () -> method.invoke(service, new HPMSAttestation(), contract));
    }

    @Test
    void updateContractMissing() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = aus.getClass().getDeclaredMethod("updateContract", HPMSOrganizationInfo.class);
        method.setAccessible(true);
        Optional<Contract> contract = (Optional<Contract>) method.invoke(aus, new HPMSOrganizationInfo());
        assertTrue(contract.isEmpty());
    }

    @Test
    void updateContract() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Contract contract =  new Contract();
        contract.setContractNumber("1");
        contract.setContractName("blank");
        contract.setHpmsParentOrgId(1L);
        contract = contractRepository.save(contract);
        Method method = aus.getClass().getDeclaredMethod("updateContract", HPMSOrganizationInfo.class);
        method.setAccessible(true);
        HPMSOrganizationInfo info = new HPMSOrganizationInfo();
        info.setContractName("test");
        info.setParentOrgId(contract.getHpmsParentOrgId().intValue());
        info.setContractId(contract.getContractNumber());
        Optional<Contract> possibleContract = (Optional<Contract>) method.invoke(aus, info);
        assertTrue(possibleContract.isPresent());
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
