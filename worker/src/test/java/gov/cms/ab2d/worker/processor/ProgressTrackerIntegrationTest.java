package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriverStub;
import gov.cms.ab2d.worker.service.FileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.concurrent.ExecutionException;

import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
class ProgressTrackerIntegrationTest {

    private static final String CONTRACT_NUMBER = "C0001";

    private JobProcessorImpl cut;

    @Autowired
    private FileService fileService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ContractProcessor contractProcessor;

    @Autowired
    private JobOutputRepository jobOutputRepository;

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    private DataSetup dataSetup;

    @Value("${patient.contract.year}")
    private int year;

    @Mock
    private LogManager eventLogger;

    @Mock
    private BFDClient bfdClient;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @BeforeEach
    void init() {

        CoverageDriver coverageDriver = new CoverageDriverStub(10, 20);

        cut = new JobProcessorImpl(fileService, jobRepository, jobOutputRepository,
                contractProcessor, coverageDriver, eventLogger);
    }

    @AfterEach
    void cleanup() {
        dataSetup.cleanup();
    }

    @Test
    void testIt() throws ExecutionException, InterruptedException {

        Contract contract = dataSetup.setupContract("C0001");

        Job job = createJob(createClient());
        job.setContract(contract);

        ProgressTracker progressTracker = ProgressTracker.builder()
                .failureThreshold(2)
                .jobUuid(job.getJobUuid())
                .expectedBeneficiaries(4)
                .build();

        org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent entry1 = BundleUtils.createBundleEntry("P1", "mbi1", year);
        org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent entry2 = BundleUtils.createBundleEntry("P2", "mbi2", year);
        org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent entry3 = BundleUtils.createBundleEntry("P3", "mbi3", year);
        org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent entry4 = BundleUtils.createBundleEntry("P4", "mbi4", year);

        org.hl7.fhir.dstu3.model.Bundle bundleA = BundleUtils.createBundle(entry1, entry2, entry3);
        org.hl7.fhir.dstu3.model.Bundle bundleB = BundleUtils.createBundle(entry1, entry2, entry3, entry4);

        when(bfdClient.requestPartDEnrolleesFromServer(STU3, CONTRACT_NUMBER, 1)).thenReturn(bundleA);
        when(bfdClient.requestPartDEnrolleesFromServer(STU3, CONTRACT_NUMBER, 2)).thenReturn(bundleB);

        cut.processContractBenes(job, progressTracker);

        Job loadedVal = jobRepository.findById(job.getId()).get();
        assertEquals(30, loadedVal.getProgress());
    }

    private PdpClient createClient() {

        PdpClient pdpClient = new PdpClient();
        pdpClient.setClientId("testclient" + 1000L);
        pdpClient.setEnabled(true);
        pdpClient.setContract(dataSetup.setupContract("W1234"));

        pdpClient = pdpClientRepository.save(pdpClient);
        dataSetup.queueForCleanup(pdpClient);
        return pdpClient;
    }

    private Job createJob(PdpClient pdpClient) {
        Job job = new Job();
        job.setJobUuid("S0000");
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage("0%");
        job.setPdpClient(pdpClient);

        job.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        job.setCreatedAt(OffsetDateTime.now());
        job.setFhirVersion(STU3);

        job = jobRepository.saveAndFlush(job);
        dataSetup.queueForCleanup(job);
        return job;
    }
}
