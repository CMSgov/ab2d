package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.worker.TestUtil;
import gov.cms.ab2d.worker.config.RoundRobinBlockingQueue;
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
import java.util.*;

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
    private PatientClaimsProcessor patientClaimsProcessor;
    @Mock
    private LogManager eventLogger;
    @Mock
    private BFDClient bfdClient;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private RoundRobinBlockingQueue<PatientClaimsRequest> requestQueue;

    @TempDir
    File tmpDirFolder;

    private ContractProcessor cut;
    private JobData jobData;
    private final String jobId = "1234";
    private final String contractId = "ABC";

    @BeforeEach
    void setup() {

        patientClaimsProcessor = new PatientClaimsProcessorImpl(bfdClient, eventLogger);
        cut = new ContractProcessorImpl(jobRepository, patientClaimsProcessor, eventLogger, requestQueue);
        ProgressTracker tracker = ProgressTracker.builder()
                .jobUuid(jobId)
                .failureThreshold(100)
                .build();

        Contract contract = new Contract();
        contract.setContractNumber(contractId);
        contract.setAttestedOn(OffsetDateTime.now().minusYears(50));
        jobData = new JobData(contract, tracker, OffsetDateTime.MIN, "Client", Collections.emptyMap());

        List<FilterOutByDate.DateRange> dates = singletonList(TestUtil.getOpenRange());
        List<CoverageSummary> summaries = List.of(
                new CoverageSummary(createIdentifierWithoutMbi("1"), null, dates),
                new CoverageSummary(createIdentifierWithoutMbi("2"), null, dates),
                new CoverageSummary(createIdentifierWithoutMbi("3"), null, dates)
        );

        tracker.addPatients(summaries.size());

        ReflectionTestUtils.setField(cut, "cancellationCheckFrequency", 20);
        ReflectionTestUtils.setField(patientClaimsProcessor, "startDate", "01/01/2020");
    }

    @Test
    void testInvalidBenes() throws IOException {
        org.hl7.fhir.dstu3.model.Bundle b1 = BundleUtils.createBundle(createBundleEntry("1"));
        org.hl7.fhir.dstu3.model.Bundle b2 = BundleUtils.createBundle(createBundleEntry("2"));
        org.hl7.fhir.dstu3.model.Bundle b4 = BundleUtils.createBundle(createBundleEntry("4"));
        when(bfdClient.requestEOBFromServer(eq(STU3), eq("1"), any())).thenReturn(b1);
        when(bfdClient.requestEOBFromServer(eq(STU3), eq("2"), any())).thenReturn(b2);
        when(bfdClient.requestEOBFromServer(eq(STU3), eq("3"), any())).thenReturn(b4);
        List<JobOutput> outputs = cut.process(tmpDirFolder.toPath(), jobData);
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
        Job job = new Job();
        job.setJobUuid(jobId);
        job.setFhirVersion(STU3);
        String val = "Hello World";

        StreamHelper helper = new TextStreamHelperImpl(
                tmpDirFolder.toPath(), contractId, 2000, 10, eventLogger, job);
        ContractData contractData = new ContractData(STU3, job, ProgressTracker.builder().build(), Collections.emptyMap());
        contractData.setStreamHelper(helper);

        ((ContractProcessorImpl) cut).writeExceptionToContractErrorFile(contractData, val, new RuntimeException("Exception"));
        String result = Files.readString(Path.of(tmpDirFolder.getAbsolutePath() + File.separator + contractId + "_error.ndjson"));
        assertEquals(val, result);
    }

    @Test
    void testWriteNullErrors() throws IOException {
        Job job = new Job();
        job.setJobUuid(jobId);
        job.setFhirVersion(STU3);

        StreamHelper helper = new TextStreamHelperImpl(
                tmpDirFolder.toPath(), contractId, 2000, 10, eventLogger, job);
        ContractData contractData = new ContractData(STU3, job, ProgressTracker.builder().build(), Collections.emptyMap());
        contractData.setStreamHelper(helper);

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
