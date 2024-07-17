package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.dto.PdpClientDTO;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.properties.PropertyServiceStub;
import gov.cms.ab2d.common.service.ContractServiceStub;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import gov.cms.ab2d.common.util.ContractServiceTestConfig;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.coverage.model.CoverageJobStatus;
import gov.cms.ab2d.coverage.model.CoverageMapping;
import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearch;
import gov.cms.ab2d.coverage.model.CoverageSearchEvent;
import gov.cms.ab2d.coverage.repository.CoverageSearchEventRepository;
import gov.cms.ab2d.coverage.repository.CoverageSearchRepository;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.coverage.util.CoverageDataSetup;
import gov.cms.ab2d.worker.config.ContractToContractCoverageMapping;
import gov.cms.ab2d.worker.service.ContractWorkerClient;
import gov.cms.ab2d.worker.service.coveragesnapshot.CoverageSnapshotService;
import gov.cms.ab2d.worker.util.WorkerDataSetup;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static gov.cms.ab2d.common.util.PropertyConstants.COVERAGE_SEARCH_STUCK_HOURS;
import static gov.cms.ab2d.common.util.PropertyConstants.COVERAGE_SEARCH_UPDATE_MONTHS;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.fhir.IdentifierUtils.BENEFICIARY_ID;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Never run internal coverage processor so this coverage processor runs unimpeded
@SpringBootTest(properties = "coverage.update.initial.delay=1000000")
@Testcontainers
@Import({AB2DSQSMockConfig.class, ContractServiceTestConfig.class})
class CoverageProcessorImplTest {

    private static final int PAST_MONTHS = 3;
    private static final int MAX_ATTEMPTS = 3;
    private static final int STUCK_HOURS = 24;

    @Container
    private static final PostgreSQLContainer postgres = new AB2DPostgresqlContainer();

    @Value("${coverage.update.max.attempts}")
    private int maxRetries;

    @Autowired
    private ContractServiceStub contractServiceStub;

    @Autowired
    private ContractWorkerClient contractWorkerClient;

    @Autowired
    private PdpClientService pdpClientService;

    @Autowired
    private CoverageSearchRepository coverageSearchRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    private CoverageService coverageService;

    private PropertiesService propertiesService = new PropertyServiceStub();

    @Autowired
    private WorkerDataSetup dataSetup;

    @Autowired
    private CoverageDataSetup coverageDataSetup;

    @Autowired
    private CoverageLockWrapper searchLock;

    @Autowired
    private ContractToContractCoverageMapping mapping;

    @Autowired
    private CoverageSnapshotService snapshotService;

    private Contract contract;
    private CoveragePeriod january;
    private CoveragePeriod february;
    private CoveragePeriod march;
    private CoveragePeriod april;

    private BFDClient bfdClient;

    private CoverageDriverImpl driver;
    private CoverageProcessorImpl processor;

    private final Map<String, String> originalValues = new HashMap<>();

    @BeforeEach
    void before() {

        // Set values used to find jobs to update
        addPropertiesTableValues();
        originalValues.clear();

        contract = dataSetup.setupContract("TST-12", AB2D_EPOCH.toOffsetDateTime());
        contractServiceStub.updateContract(contract);

        january = coverageDataSetup.createCoveragePeriod("TST-12", 1, 2020);
        february = coverageDataSetup.createCoveragePeriod("TST-12", 2, 2020);
        march = coverageDataSetup.createCoveragePeriod("TST-12", 3, 2020);
        april = coverageDataSetup.createCoveragePeriod("TST-12", 4, 2020);

        PdpClientDTO contractPdpClient = createClient(contract.toDTO(), "TST-12", SPONSOR_ROLE);
        pdpClientService.createClient(contractPdpClient);
        dataSetup.queueForCleanup(pdpClientService.getClientById("TST-12"));

        bfdClient = mock(BFDClient.class);

        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(6);
        taskExecutor.setCorePoolSize(3);
        taskExecutor.initialize();

        processor = new CoverageProcessorImpl(coverageService, bfdClient, taskExecutor, MAX_ATTEMPTS, contractWorkerClient, snapshotService);
        driver = new CoverageDriverImpl(coverageSearchRepo, pdpClientService, coverageService, propertiesService, processor, searchLock, mapping, snapshotService);
    }

    @AfterEach
    void cleanup() {
        processor.shutdown();
        dataSetup.cleanup();
        coverageDataSetup.cleanup();
        originalValues.entrySet().stream().forEach(c -> propertiesService.updateProperty(c.getKey(), c.getValue()));
    }

    @Test
    void testQueueCoveragePeriod1() {
        assertEquals(0, coverageSearchRepo.count());
        processor.queueCoveragePeriod(january, false);
        assertEquals(1, coverageSearchRepo.count());
    }

    // test code path for prioritize true
    @Test
    void testQueueCoveragePeriod2() {
        assertEquals(0, coverageSearchRepo.count());
        processor.queueCoveragePeriod(january, true);
        assertEquals(1, coverageSearchRepo.count());
    }

    // test code path for returning early due to shutdown
    @Test
    void testQueueCoveragePeriod3() {
        assertEquals(0, coverageSearchRepo.count());
        processor.shutdown();
        processor.queueCoveragePeriod(january, true);
        assertEquals(0, coverageSearchRepo.count());
    }

    @Test
    void testStartJob1() {
      CoverageSearchEvent event = new CoverageSearchEvent();
      event.setCoveragePeriod(january);
      CoverageSearch search = new CoverageSearch();
      search.setPeriod(january);
      CoverageMapping coverageMapping = new CoverageMapping(event, search);

      assertTrue(processor.startJob(coverageMapping));
    }

    // test code path for returning early due to shutdown
    @Test
    void testStartJob2() {
      CoverageSearchEvent event = new CoverageSearchEvent();
      event.setCoveragePeriod(january);
      CoverageSearch search = new CoverageSearch();
      search.setPeriod(january);
      CoverageMapping coverageMapping = new CoverageMapping(event, search);

      processor.shutdown();
      assertFalse(processor.startJob(coverageMapping));
    }

    private void addPropertiesTableValues() {
      originalValues.put(COVERAGE_SEARCH_UPDATE_MONTHS, propertiesService.getProperty(COVERAGE_SEARCH_UPDATE_MONTHS, "3"));
      originalValues.put(COVERAGE_SEARCH_STUCK_HOURS, propertiesService.getProperty(COVERAGE_SEARCH_STUCK_HOURS, "24"));

      propertiesService.updateProperty(COVERAGE_SEARCH_UPDATE_MONTHS, "" + PAST_MONTHS);
      propertiesService.updateProperty(COVERAGE_SEARCH_STUCK_HOURS, "" + STUCK_HOURS);
    }

    private PdpClientDTO createClient(ContractDTO contract, String clientId, @Nullable String roleName) {
        PdpClientDTO client = new PdpClientDTO();
        client.setClientId(clientId);
        client.setOrganization(clientId);
        client.setEnabled(true);
        client.setContract(contract);
        client.setRole(roleName);

        return client;
    }
}
