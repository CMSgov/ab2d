package gov.cms.ab2d.testjobs;

import gov.cms.ab2d.AB2DPostgresqlContainer;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageMapping;
import gov.cms.ab2d.common.model.CoverageSearch;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.model.SinceSource;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.CoverageSearchRepository;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.service.CoverageService;
import gov.cms.ab2d.common.service.InvalidContractException;
import gov.cms.ab2d.common.service.JobOutputService;
import gov.cms.ab2d.common.service.JobService;
import gov.cms.ab2d.common.service.JobServiceImpl;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventSummary;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.fhir.IdentifierUtils;
import gov.cms.ab2d.fhir.PatientIdentifier;
import gov.cms.ab2d.worker.processor.ContractProcessor;
import gov.cms.ab2d.worker.processor.JobPreProcessor;
import gov.cms.ab2d.worker.processor.JobPreProcessorImpl;
import gov.cms.ab2d.worker.processor.JobProcessor;
import gov.cms.ab2d.worker.processor.JobProcessorImpl;
import gov.cms.ab2d.worker.processor.JobProgressService;
import gov.cms.ab2d.worker.processor.JobProgressUpdateService;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriverImpl;
import gov.cms.ab2d.worker.processor.coverage.CoverageLockWrapper;
import gov.cms.ab2d.worker.processor.coverage.CoverageProcessor;
import gov.cms.ab2d.worker.processor.coverage.CoverageProcessorImpl;
import gov.cms.ab2d.worker.service.FileServiceImpl;
import gov.cms.ab2d.worker.service.JobChannelService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static gov.cms.ab2d.fhir.FhirVersion.R4;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * This is an end to end test for a Synthea contract which also tests the default _since behavior for the R4 API
 *
 * It goes through the whole life cycle of several jobs, calculating the new default since depending on if the
 * previous job is successful and had downloaded all its data.
 *
 * In the db container, it generates coverage data for the contract, pre-processes, then processes each job
 * (except the last one). All the data pulls from BFDs sandbox Synthea data.
 */
@SpringBootTest
@Testcontainers
@Slf4j
@ExtendWith(MockitoExtension.class)
public class EndToEndBfdTests {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    // We don't care about logging here
    @Mock
    LogManager logManager;

    @Autowired
    private BFDClient client;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private ContractRepository contractRepository;
    @Autowired
    private JobChannelService jobChannelService;
    @Autowired
    private JobProgressService jobProgressService;
    @Autowired
    private JobProgressUpdateService jobProgressUpdateService;
    @Autowired
    private JobOutputRepository jobOutputRepository;
    @Autowired
    private ContractProcessor contractProcessor;
    @Autowired
    private CoverageSearchRepository coverageSearchRepository;
    @Autowired
    private PdpClientService pdpClientService;
    @Autowired
    private CoverageService coverageService;
    @Autowired
    private CoverageProcessor coverageProcessor;
    @Autowired
    private CoverageLockWrapper coverageLockWrapper;
    @Autowired
    private PropertiesService propertiesService;
    @Autowired
    private PdpClientRepository pdpClientRepository;
    @Autowired
    private JobOutputService jobOutputService;
    @Autowired
    private LoggerEventSummary logEventSummary;

    private JobService jobService;
    private CoverageDriver coverageDriver;
    private JobPreProcessor jobPreProcessor;
    private JobProcessor jobProcessor;

    private final String path = System.getProperty("java.io.tmpdir");

    private static final String CONTRACT_TO_USE = "Z1007";
    private static final String CONTRACT_TO_USE_CLIENT_ID = "KtmekgkCTalQkGue2B-0Z0hGC1Dk7khtJ30XMI3J";

    @BeforeEach
    void setUp() {

        PropertiesDTO coreClaimsPool = new PropertiesDTO();
        coreClaimsPool.setKey(Constants.PCP_CORE_POOL_SIZE);
        coreClaimsPool.setValue("20");

        PropertiesDTO maxClaimsPool = new PropertiesDTO();
        maxClaimsPool.setKey(Constants.PCP_MAX_POOL_SIZE);
        maxClaimsPool.setValue("30");

        PropertiesDTO scaleToMaxTime = new PropertiesDTO();
        scaleToMaxTime.setKey(Constants.PCP_SCALE_TO_MAX_TIME);
        scaleToMaxTime.setValue("10");

        propertiesService.updateProperties(List.of(coreClaimsPool, maxClaimsPool, scaleToMaxTime));

        coverageDriver = new CoverageDriverImpl(coverageSearchRepository, pdpClientService, coverageService,
                propertiesService, coverageProcessor, coverageLockWrapper);

        // Instantiate the job processors
        jobService = new JobServiceImpl(pdpClientService, jobRepository, jobOutputService, logManager, logEventSummary, path);
        jobPreProcessor = new JobPreProcessorImpl(jobRepository, logManager, coverageDriver, false);
        ReflectionTestUtils.setField(jobPreProcessor, "skipBillablePeriodCheck", false);

        jobProcessor = new JobProcessorImpl(new FileServiceImpl(), jobChannelService, jobProgressService, jobProgressUpdateService,
                jobRepository, jobOutputRepository, contractProcessor, coverageDriver, logManager);
        ReflectionTestUtils.setField(jobProcessor, "failureThreshold", 10);
        ReflectionTestUtils.setField(jobProcessor, "efsMount", path);

        // Set up the PDP client
    }

    /**
     *  Run a bunch of jobs with different scenarios to test the default _since capabilities. To run the jobs,
     *  we first need to do some setup:
     *
     *  1. Disable all contracts except the one we want to use
     *  2. Load all the coverage data for that contract
     *  3. Run the jobs
     *  4. Clean up files for the jobs if necessary
     */
    @Test
    void runJobs() throws InterruptedException {
        PdpClient pdpClient = setupClient(getContract());

        final String path = System.getProperty("java.io.tmpdir");

        // So we don't load coverage data for all the contracts we need, disable all but the one we want
        disableContractWeDontNeed();

        // Get all the coverage data for all enabled contracts
        getCoverage();

        // -------------- FIRST JOB --------------------

        Job firstJob = createJob(pdpClient);
        String firstJobId = firstJob.getJobUuid();
        OffsetDateTime firstTime = firstJob.getCreatedAt();

        firstJob = jobPreProcessor.preprocess(firstJob.getJobUuid());

        Assertions.assertEquals(SinceSource.FIRST_RUN, firstJob.getSinceSource());
        assertNull(firstJob.getSince());

        firstJob = jobProcessor.process(firstJob.getJobUuid());
        List<JobOutput> jobOutputs1 = firstJob.getJobOutputs();
        assertNotNull(jobOutputs1);
        Assertions.assertEquals(JobStatus.SUCCESSFUL, firstJob.getStatus());
        assertTrue(jobOutputs1.size() > 0);
        jobOutputs1.forEach(f -> downloadFile(path, firstJobId, f.getFilePath()));

        // -------------- SECOND JOB --------------------

        Job secondJob = createJob(pdpClient);
        String secondJobId = secondJob.getJobUuid();

        secondJob = jobPreProcessor.preprocess(secondJob.getJobUuid());
        Assertions.assertEquals(SinceSource.AB2D, secondJob.getSinceSource());
        Assertions.assertEquals(firstTime.getNano(), secondJob.getSince().getNano());

        // Set the since to a value that will give us output
        OffsetDateTime newTime = OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        secondJob.setSince(newTime);
        jobRepository.save(secondJob);

        secondJob = jobProcessor.process(secondJob.getJobUuid());
        List<JobOutput> jobOutputs2 = secondJob.getJobOutputs();

        assertTrue(jobOutputs2.size() > 0);

        // -------------- THIRD JOB --------------------

        Job thirdJob = createJob(pdpClient);
        OffsetDateTime thirdTime = thirdJob.getCreatedAt();

        thirdJob = jobPreProcessor.preprocess(thirdJob.getJobUuid());
        Assertions.assertEquals(SinceSource.AB2D, thirdJob.getSinceSource());
        // We never downloaded the second job's files so the last successful job is job 1
        Assertions.assertEquals(firstTime.getNano(), thirdJob.getSince().getNano());

        // Since the last job time was just a couple of minutes ago (Job 1), there should be no incremental data
        thirdJob = jobProcessor.process(thirdJob.getJobUuid());
        List<JobOutput> jobOutputs3 = thirdJob.getJobOutputs();
        assertTrue(jobOutputs3 == null || jobOutputs3.isEmpty());
        assertNotNull(jobOutputs3);

        // -------------- FOURTH JOB --------------------

        Job fourthJob = createJob(pdpClient);

        fourthJob = jobPreProcessor.preprocess(fourthJob.getJobUuid());
        Assertions.assertEquals(SinceSource.AB2D, fourthJob.getSinceSource());
        // We never downloaded the second job's files, but there were none in the third so it should pick that.
        Assertions.assertEquals(thirdTime.getNano(), fourthJob.getSince().getNano());

        // -------------- CLEAN UP --------------------
        // Now delete the files from job 2
        jobOutputs2.forEach(f -> downloadFile(path, secondJobId, f.getFilePath()));
    }

    /**
     * Call the service to mark the file as downloaded and delete the file
     *
     * @param path - the directory all job data is stored under
     * @param jobUuid - the job ID
     * @param filename - the name of the file (without a path)
     */
    private void downloadFile(String path, String jobUuid, String filename) {
        try {
            Path file = Paths.get(path, jobUuid, filename);
            Resource resource = new UrlResource(file.toUri());
            jobService.deleteFileForJob(resource.getFile(), jobUuid);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to delete file " + filename, ex);
        }
    }

    /**
     * For all but the contract we want to use, disable them
     */
    private void disableContractWeDontNeed() {
        List<PdpClient> clients = pdpClientRepository.findAllByEnabledTrue().stream()
                .filter(client -> client.getContract().getAttestedOn() != null)
                .collect(toList());
        for (PdpClient pdp : clients) {
            if (!pdp.getContract().getContractNumber().equals(CONTRACT_TO_USE)) {
                pdp.setEnabled(false);
                pdpClientRepository.save(pdp);
            }
        }
    }

    /**
     * Load the coverage data for all enabled contracts. To do this we:
     *
     * 1. Discover all the coverage periods for the contracts
     * 2. Queue all the stale coverage periods to searches
     * 3. For each search, start it
     * 4. While the searches are not complete, call monitorMappingJobs which takes the results of the searches and adds
     *    them to the queue to save. This would be done by a quartz job normally
     * 5. While the searches have not been saved, call insertJobResults which takes the results of the save queue and *
     *    adds saves the data. This would be done by a quartz job normally
     *
     *  These operations are done sequentially and it's fine for a small amount of data. This is not appropriate
     *  for several or large contracts
     *
     * @throws InterruptedException if there is an issue with threads being interrupted
     */
    private void getCoverage() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        coverageDriver.discoverCoveragePeriods();
        coverageDriver.queueStaleCoveragePeriods();
        Optional<CoverageSearch> search = ((CoverageDriverImpl) coverageDriver).getNextSearch();
        while (search.isPresent()) {

            Optional<CoverageMapping> maybeSearch = coverageService.startSearch(search.get(), "starting a job");
            if (maybeSearch.isEmpty()) {
                continue;
            }

            CoverageMapping mapping = maybeSearch.get();

            if (!coverageProcessor.startJob(mapping)) {
                coverageService.cancelSearch(mapping.getPeriodId(), "failed to start job");
                coverageProcessor.queueMapping(mapping, false);
            }
            search = ((CoverageDriverImpl) coverageDriver).getNextSearch();
        }

        // Wait for all the searches to be done
        while (((CoverageProcessorImpl) coverageProcessor).getNumberInProgressMappings() > 0) {
            System.out.println("\n************** " + ((CoverageProcessorImpl) coverageProcessor).getNumberInProgressMappings() + " num searches still to do\n");
            ((CoverageProcessorImpl) coverageProcessor).monitorMappingJobs();
            Thread.sleep(1000);
        }

        // Wait for all the inserts to be done
        while (((CoverageProcessorImpl) coverageProcessor).getNumberCoverageInsertion() > 0) {
            System.out.println("\n************** " + ((CoverageProcessorImpl) coverageProcessor).getNumberCoverageInsertion() + " num inserts still to do\n");
            ((CoverageProcessorImpl) coverageProcessor).insertJobResults();
            Thread.sleep(1000);
        }

        long endTime = System.currentTimeMillis();
        long timeToProcess = endTime - startTime;
        System.out.println("\n*************** It took " + ((double) timeToProcess) / 1000 + " seconds to load coverage data\n");
    }

    private Contract getContract() {
        return contractRepository.findContractByContractNumber(EndToEndBfdTests.CONTRACT_TO_USE).orElse(null);
    }

    private PdpClient setupClient(Contract contract) {
        PdpClient pdpClient = new PdpClient();
        pdpClient.setClientId(EndToEndBfdTests.CONTRACT_TO_USE_CLIENT_ID);
        pdpClient.setOrganization("Synthea Data");
        pdpClient.setEnabled(true);
        pdpClient.setContract(contract);
        return pdpClientRepository.save(pdpClient);
    }

    private Job createJob(PdpClient pdpClient) {
        // Populate the data with a fictitious URL, the contract we want, no since date and we need R4 to
        // trigger the default since stuff
        return createJob(EOB, "http://localhost:8080/api/v2/fhir/Group/{contractNumber}/$export", CONTRACT_TO_USE,
                "application/ndjson", null, FhirVersion.R4, pdpClient);
    }

    public Job createJob(String resourceTypes, String url, String contractNumber, String outputFormat,
                         OffsetDateTime since, FhirVersion version, PdpClient pdpClient) {
        Job job = new Job();
        job.setResourceTypes(resourceTypes);
        job.setJobUuid(UUID.randomUUID().toString());
        job.setRequestUrl(url);
        job.setStatusMessage(JobServiceImpl.INITIAL_JOB_STATUS_MESSAGE);
        job.setCreatedAt(OffsetDateTime.now());
        job.setOutputFormat(outputFormat);
        job.setProgress(0);
        job.setSince(since);
        job.setFhirVersion(version);
        job.setPdpClient(pdpClient);

        // Check to see if there is any attestation
        Contract contract = pdpClient.getContract();
        if (contractNumber != null && !contractNumber.equals(contract.getContractNumber())) {
            String errorMsg = "Specifying contract: " + contractNumber + " not associated with internal id: " + pdpClient.getId();
            throw new InvalidContractException(errorMsg);
        }

        if (!contract.hasAttestation()) {
            String errorMsg = "Contract: " + contractNumber + " is not attested.";
            throw new InvalidContractException(errorMsg);
        }

        job.setContract(contract);
        job.setStatus(JobStatus.SUBMITTED);
        return jobRepository.save(job);
    }

    @ParameterizedTest
    @MethodSource("getVersion")
    public void testPatientEndpoint(FhirVersion version, String contract, int month, int year) {
        BFDClient.BFD_BULK_JOB_ID.set("TEST");

        log.info("Testing IDs for " + version.toString());
        List<PatientIdentifier> patientIds = new ArrayList<>();

        log.info(String.format("Do Request for %s for %02d/%04d", contract, month, year));
        IBaseBundle bundle = client.requestPartDEnrolleesFromServer(version, contract, month, year);
        assertNotNull(bundle);
        int numberOfBenes = BundleUtils.getEntries(bundle).size();
        patientIds.addAll(extractIds(bundle, version));
        log.info("Found: " + numberOfBenes + " benes");

        while (BundleUtils.getNextLink(bundle) != null) {
            log.info(String.format("Do Next Request for %s for %02d/%04d", contract, month, year));
            bundle = client.requestNextBundleFromServer(version, bundle);
            numberOfBenes += BundleUtils.getEntries(bundle).size();
            log.info("Found: " + numberOfBenes + " benes");
            patientIds.addAll(extractIds(bundle, version));
        }

        log.info("Contract: " + contract + " has " + numberOfBenes + " benes with " + patientIds.size() + " ids");
        assertTrue(patientIds.size() >= 1000);
        assertEquals(0, patientIds.size() % 1000);
        assertTrue(patientIds.size() >= (2 * numberOfBenes));
    }

    public static List<PatientIdentifier> extractIds(IBaseBundle bundle, FhirVersion version) {
        List<PatientIdentifier> ids = new ArrayList<>();
        List patients = BundleUtils.getPatientStream(bundle, version)
                .collect(Collectors.toList());
        patients.forEach(c -> ids.addAll(IdentifierUtils.getIdentifiers((IDomainResource) c)));
        return ids;
    }

    /**
     * Return the different versions of FHIR to test against
     *
     * @return the stream of FHIR versions
     */
    static Stream<Arguments> getVersion() {
        if (v2Enabled()) {
            return Stream.of(arguments(STU3, "Z0001", 1, 3), arguments(R4, "Z0001", 1, 3));
        } else {
            return Stream.of(arguments(STU3, "Z0001", 1, 3));
        }
    }

    private static boolean v2Enabled() {
        String v2Enabled = System.getenv("AB2D_V2_ENABLED");
        if (v2Enabled != null && v2Enabled.equalsIgnoreCase("true")) {
            return true;
        }
        return false;
    }
}
