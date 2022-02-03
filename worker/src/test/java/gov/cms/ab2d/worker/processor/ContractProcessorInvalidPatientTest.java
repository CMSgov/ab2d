package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.worker.TestUtil;
import gov.cms.ab2d.worker.config.ContractToContractCoverageMapping;
import gov.cms.ab2d.worker.config.RoundRobinBlockingQueue;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.repository.StubContractRepository;
import gov.cms.ab2d.worker.repository.StubJobRepository;
import gov.cms.ab2d.worker.service.JobChannelService;
import gov.cms.ab2d.worker.service.JobChannelStubServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifierWithoutMbi;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
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
    private LogManager eventLogger;

    @Mock
    private BFDClient bfdClient;

    @Mock
    private ContractToContractCoverageMapping mapping;

    private ContractRepository contractRepository;

    private JobRepository jobRepository;

    @Mock
    private RoundRobinBlockingQueue<PatientClaimsRequest> requestQueue;

    @TempDir
    File tmpDirFolder;

    private ContractProcessor cut;
    private final Contract contract = new Contract();
    private final Job job = new Job();
    private static final String jobId = "1234";
    private final String contractId = "ABC";

    @BeforeEach
    void setup() {


        Contract contract = new Contract();
        contract.setContractNumber(contractId);
        contract.setAttestedOn(OffsetDateTime.now().minusYears(50));

        contractRepository = new StubContractRepository(contract);

        job.setJobUuid(jobId);
        job.setContractNumber(contract.getContractNumber());
        jobRepository = new StubJobRepository(job);

        patientClaimsProcessor = new PatientClaimsProcessorImpl(bfdClient, eventLogger);
        JobProgressServiceImpl jobProgressUpdateService  = new JobProgressServiceImpl(jobRepository);
        jobProgressUpdateService.initJob(jobId);
        JobChannelService jobChannelService = new JobChannelStubServiceImpl(jobProgressUpdateService);
        cut = new ContractProcessorImpl(contractRepository, jobRepository, coverageDriver, patientClaimsProcessor, eventLogger,
                requestQueue, jobChannelService, jobProgressUpdateService,mapping);
        jobChannelService.sendUpdate(jobId, JobMeasure.FAILURE_THRESHHOLD, 100);

        ReflectionTestUtils.setField(patientClaimsProcessor, "earliestDataDate", "01/01/2020");
    }

    @Test
    void testInvalidBenes() throws IOException {
        when(mapping.map(any(Contract.class))).thenReturn(new ContractForCoverageDTO(contract.getContractNumber(), contract.getAttestedOn(), ContractForCoverageDTO.ContractType.NORMAL));
        org.hl7.fhir.dstu3.model.Bundle b1 = BundleUtils.createBundle(createBundleEntry("1"));
        org.hl7.fhir.dstu3.model.Bundle b2 = BundleUtils.createBundle(createBundleEntry("2"));
        org.hl7.fhir.dstu3.model.Bundle b4 = BundleUtils.createBundle(createBundleEntry("4"));
        when(bfdClient.requestEOBFromServer(eq(STU3), eq(1L), any())).thenReturn(b1);
        when(bfdClient.requestEOBFromServer(eq(STU3), eq(2L), any())).thenReturn(b2);
        when(bfdClient.requestEOBFromServer(eq(STU3), eq(3L), any())).thenReturn(b4);

        when(coverageDriver.numberOfBeneficiariesToProcess(any(Job.class), any(Contract.class))).thenReturn(3);

        List<FilterOutByDate.DateRange> dates = singletonList(TestUtil.getOpenRange());
        List<CoverageSummary> summaries = List.of(new CoverageSummary(createIdentifierWithoutMbi(1L), null, dates),
                new CoverageSummary(createIdentifierWithoutMbi(2L), null, dates),
                new CoverageSummary(createIdentifierWithoutMbi(3L), null, dates));
        when(coverageDriver.pageCoverage(any(CoveragePagingRequest.class))).thenReturn(
                new CoveragePagingResult(summaries, null));

        List<JobOutput> outputs = cut.process(tmpDirFolder.toPath(), job);

        assertNotNull(outputs);
        assertEquals(2, outputs.size());

        String fileName1 = contractId + "_0001.ndjson";
        String fileName2 = contractId + "_0002.ndjson";
        String output1 = outputs.get(0).getFilePath();
        String output2 = outputs.get(1).getFilePath();

        assertTrue(output1.equalsIgnoreCase(fileName1) || output1.equalsIgnoreCase(fileName2));
        assertTrue(output2.equalsIgnoreCase(fileName1) || output2.equalsIgnoreCase(fileName2));

        String actual1 = Files.readString(Path.of(tmpDirFolder.getAbsolutePath() + File.separator + output1));
        String actual2 = Files.readString(Path.of(tmpDirFolder.getAbsolutePath() + File.separator + output2));

        assertTrue(actual1.contains("Patient/1") || actual1.contains("Patient/2"));
        assertTrue(actual2.contains("Patient/1") || actual2.contains("Patient/2"));
    }

    @Test
    void testWriteErrors() throws IOException {

        String val = "Hello World";

        StreamHelper helper = new TextStreamHelperImpl(
                tmpDirFolder.toPath(), contractId, 2000, 10, eventLogger, job);
        ContractData contractData = new ContractData(contract, job, helper);

        ((ContractProcessorImpl) cut).writeExceptionToContractErrorFile(contractData, val, new RuntimeException("Exception"));
        String result = Files.readString(Path.of(tmpDirFolder.getAbsolutePath() + File.separator + contractId + "_error.ndjson"));
        assertEquals(val, result);
    }

    @Test
    void testWriteNullErrors() throws IOException {
        StreamHelper helper = new TextStreamHelperImpl(
                tmpDirFolder.toPath(), contractId, 2000, 10, eventLogger, job);
        ContractData contractData = new ContractData(contract, job, helper);

        ((ContractProcessorImpl) cut).writeExceptionToContractErrorFile(contractData, null, new RuntimeException("Exception"));
        assertThrows(NoSuchFileException.class, () -> Files.readString(Path.of(tmpDirFolder.getAbsolutePath() + File.separator + contractId + "_error.ndjson")));
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

    public static org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent createBundleEntry(String patientId) {
        var component = new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent();
        component.setResource(createEOB(patientId));
        return component;
    }
}
