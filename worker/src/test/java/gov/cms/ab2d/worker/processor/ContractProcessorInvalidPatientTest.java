package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.util.GzipCompressUtils;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobOutput;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.worker.TestUtil;
import gov.cms.ab2d.worker.config.ContractToContractCoverageMapping;
import gov.cms.ab2d.worker.config.RoundRobinBlockingQueue;
import gov.cms.ab2d.worker.config.SearchConfig;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.repository.StubJobRepository;
import gov.cms.ab2d.worker.service.JobChannelService;
import gov.cms.ab2d.worker.service.JobChannelStubServiceImpl;
import gov.cms.ab2d.worker.util.ContractWorkerClientMock;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;


import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.fhir.FhirVersion.R4V3;
import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifierWithoutMbi;
import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifierWithoutMbi_V3;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractProcessorInvalidPatientTest {

    @Mock
    private CoverageDriver coverageDriver;

    @Mock
    private PatientClaimsProcessor patientClaimsProcessor;

    @Mock
    private SQSEventClient eventLogger;

    @Mock
    private BFDClient bfdClient;

    @Mock
    private ContractToContractCoverageMapping mapping;

    private ContractWorkerClientMock contractWorkerClient;

    @Mock
    private RoundRobinBlockingQueue<PatientClaimsRequest> requestQueue;

    @TempDir
    File tmpDirFolder;

    private ContractProcessor cut;
    private ContractDTO contract;
    private Job job;
    private static final String jobId = "1234";
    private final String contractId = "ABC";
    private static final String FINISHED_DIR = "finishedDir";
    private static final String STREAMING_DIR = "streamingDir";

    @BeforeEach
    void setup() {
        contractWorkerClient = new ContractWorkerClientMock();
        contract = new ContractDTO(null, contractId, contractId, OffsetDateTime.now().minusYears(50), null, 0, 0);
        initialize();
    }

    private void initialize() {
        initialize(false);
    }

    private void initialize(boolean isV3Job) {
        job = new Job();
        if (isV3Job) {
            job.setRequestUrl(".../v3/...");
        }
        job.setJobUuid(jobId);
        job.setContractNumber(contract.getContractNumber());
        JobRepository jobRepository = new StubJobRepository(job);

        SearchConfig searchConfig = new SearchConfig(tmpDirFolder.getAbsolutePath(), STREAMING_DIR,
                FINISHED_DIR, 0, 0, 1, 2);
        patientClaimsProcessor = new PatientClaimsProcessorImpl(bfdClient, eventLogger, searchConfig);
        JobProgressServiceImpl jobProgressUpdateService = new JobProgressServiceImpl(jobRepository);
        jobProgressUpdateService.initJob(jobId);
        JobChannelService jobChannelService = new JobChannelStubServiceImpl(jobProgressUpdateService);

        ThreadPoolTaskExecutor aggTP = new ThreadPoolTaskExecutor();
        aggTP.initialize();

        cut = new ContractProcessorImpl(contractWorkerClient, jobRepository, coverageDriver, patientClaimsProcessor, eventLogger,
                requestQueue, jobChannelService, jobProgressUpdateService, mapping, aggTP, searchConfig);

        ReflectionTestUtils.setField(cut, "eobJobPatientQueueMaxSize", 1);
        ReflectionTestUtils.setField(cut, "eobJobPatientQueuePageSize", 1);
        jobChannelService.sendUpdate(jobId, JobMeasure.FAILURE_THRESHHOLD, 100);

        ReflectionTestUtils.setField(patientClaimsProcessor, "earliestDataDate", "01/01/2020");
    }

    @Test
    @DisplayName("Test invalid benes")
    void testInvalidBenes() throws IOException {
        when(mapping.map(any(ContractDTO.class))).thenReturn(new ContractForCoverageDTO(contract.getContractNumber(), contract.getAttestedOn(), ContractForCoverageDTO.ContractType.NORMAL));
        org.hl7.fhir.dstu3.model.Bundle b1 = BundleUtils.createBundle(createBundleEntry("1"));
        org.hl7.fhir.dstu3.model.Bundle b2 = BundleUtils.createBundle(createBundleEntry("2"));
        org.hl7.fhir.dstu3.model.Bundle b4 = BundleUtils.createBundle(createBundleEntry("4"));
        when(bfdClient.requestEOBFromServer(eq(STU3), eq(1L), any(), any(), any())).thenReturn(b1);
        when(bfdClient.requestEOBFromServer(eq(STU3), eq(2L), any(), any(), any())).thenReturn(b2);
        when(bfdClient.requestEOBFromServer(eq(STU3), eq(3L), any(), any(), any())).thenReturn(b4);

        when(coverageDriver.numberOfBeneficiariesToProcess(any(Job.class), any(ContractDTO.class))).thenReturn(3);

        List<FilterOutByDate.DateRange> dates = singletonList(TestUtil.getOpenRange());
        List<CoverageSummary> summaries = List.of(new CoverageSummary(createIdentifierWithoutMbi(1L), null, dates),
                new CoverageSummary(createIdentifierWithoutMbi(2L), null, dates),
                new CoverageSummary(createIdentifierWithoutMbi(3L), null, dates));
        when(coverageDriver.pageCoverage(any(CoveragePagingRequest.class))).thenReturn(
                new CoveragePagingResult(summaries, null));

        List<JobOutput> outputs = cut.process(job);

        assertNotNull(outputs);
        assertEquals(1, outputs.size());

        String fileName1AfterCompressing = contractId + "_0001.ndjson.gz";
        String output1 = outputs.get(0).getFilePath();
        assertTrue(output1.equalsIgnoreCase(fileName1AfterCompressing));

        // verify original output file is deleted after compressing
        String fileName1BeforeCompressing = contractId + "_0001.ndjson";
        val originalOutputFile = new File(tmpDirFolder.getAbsolutePath() + File.separator + job.getJobUuid() + "/" + fileName1BeforeCompressing);
        assertFalse(originalOutputFile.exists());

        // decompress output file and write decompressed contents to `outputFileDecompressed`
        val outputFileCompressed = Path.of(tmpDirFolder.getAbsolutePath() + File.separator + job.getJobUuid() + "/" + output1);
        val outputFileDecompressedStream = new ByteArrayOutputStream();
        GzipCompressUtils.decompress(outputFileCompressed, outputFileDecompressedStream);
        String actual1 = outputFileDecompressedStream.toString(Charset.defaultCharset());

        assertTrue(actual1.contains("Patient/1") && actual1.contains("Patient/2"));
        assertFalse(actual1.contains("Patient/3") || actual1.contains("Patient/4"));
    }

    @Disabled("Wait for V3 trimmer")
    @Test
    @DisplayName("V3 - Test invalid benes")
    void testInvalidBenes_V3() throws IOException {
        initialize(true);
        when(mapping.map(any(ContractDTO.class))).thenReturn(new ContractForCoverageDTO(contract.getContractNumber(), contract.getAttestedOn(), ContractForCoverageDTO.ContractType.NORMAL));
        org.hl7.fhir.r4.model.Bundle b1 = BundleUtils.createBundle_R4(createBundleEntry_R4("1"));
        org.hl7.fhir.r4.model.Bundle b2 = BundleUtils.createBundle_R4(createBundleEntry_R4("2"));
        org.hl7.fhir.r4.model.Bundle b4 = BundleUtils.createBundle_R4(createBundleEntry_R4("4"));

        when(bfdClient.requestEOBFromServer(eq(R4V3), eq(1L), any(), any(), any())).thenReturn(b1);
        when(bfdClient.requestEOBFromServer(eq(R4V3), eq(2L), any(), any(), any())).thenReturn(b2);
        when(bfdClient.requestEOBFromServer(eq(R4V3), eq(3L), any(), any(), any())).thenReturn(b4);

        // Note: numberOfBeneficiariesToProcessV3
        when(coverageDriver.numberOfBeneficiariesToProcessV3(any(Job.class), any(ContractDTO.class))).thenReturn(3);

        List<FilterOutByDate.DateRange> dates = singletonList(TestUtil.getOpenRange());
        // Note: createIdentifierWithoutMbi_V3
        List<CoverageSummary> summaries = List.of(new CoverageSummary(createIdentifierWithoutMbi_V3(1L), null, dates),
                new CoverageSummary(createIdentifierWithoutMbi_V3(2L), null, dates),
                new CoverageSummary(createIdentifierWithoutMbi_V3(3L), null, dates));
        // Note: pageCoverageV3
        when(coverageDriver.pageCoverageV3(any(CoveragePagingRequest.class))).thenReturn(
                new CoveragePagingResult(summaries, null));

        List<JobOutput> outputs = cut.process(job);

        assertNotNull(outputs);
        // TODO Re-evaluate when V3 trimmer is complete -- outputs is currently empty
        assertEquals(1, outputs.size());

        String fileName1AfterCompressing = contractId + "_0001.ndjson.gz";
        String output1 = outputs.get(0).getFilePath();
        assertTrue(output1.equalsIgnoreCase(fileName1AfterCompressing));

        // verify original output file is deleted after compressing
        String fileName1BeforeCompressing = contractId + "_0001.ndjson";
        val originalOutputFile = new File(tmpDirFolder.getAbsolutePath() + File.separator + job.getJobUuid() + "/" + fileName1BeforeCompressing);
        assertFalse(originalOutputFile.exists());

        // decompress output file and write decompressed contents to `outputFileDecompressed`
        val outputFileCompressed = Path.of(tmpDirFolder.getAbsolutePath() + File.separator + job.getJobUuid() + "/" + output1);
        val outputFileDecompressedStream = new ByteArrayOutputStream();
        GzipCompressUtils.decompress(outputFileCompressed, outputFileDecompressedStream);
        String actual1 = outputFileDecompressedStream.toString(Charset.defaultCharset());

        assertTrue(actual1.contains("Patient/1") && actual1.contains("Patient/2"));
        assertFalse(actual1.contains("Patient/3") || actual1.contains("Patient/4"));
    }


    private static org.hl7.fhir.dstu3.model.ExplanationOfBenefit createEOB(String patientId) {
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit b = new org.hl7.fhir.dstu3.model.ExplanationOfBenefit();
        org.hl7.fhir.dstu3.model.Period p = new org.hl7.fhir.dstu3.model.Period();
        p.setStart(new Date(0));
        p.setEnd(new Date());
        b.setBillablePeriod(p);
        org.hl7.fhir.dstu3.model.Reference ref = new org.hl7.fhir.dstu3.model.Reference("Patient/" + patientId);
        b.setPatient(ref);
        return b;
    }

    private static org.hl7.fhir.r4.model.ExplanationOfBenefit createEOB_R4(String patientId) {
        org.hl7.fhir.r4.model.ExplanationOfBenefit b = new org.hl7.fhir.r4.model.ExplanationOfBenefit();
        org.hl7.fhir.r4.model.Period p = new org.hl7.fhir.r4.model.Period();
        p.setStart(new Date(0));
        p.setEnd(new Date());
        b.setBillablePeriod(p);
        org.hl7.fhir.r4.model.Reference ref = new org.hl7.fhir.r4.model.Reference("Patient/" + patientId);
        b.setPatient(ref);
        return b;
    }

    public static org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent createBundleEntry(String patientId) {
        var component = new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent();
        component.setResource(createEOB(patientId));
        return component;
    }

    public static org.hl7.fhir.r4.model.Bundle.BundleEntryComponent createBundleEntry_R4(String patientId) {
        var component = new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent();
        component.setResource(createEOB_R4(patientId));
        return component;
    }
}
