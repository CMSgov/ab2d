package gov.cms.ab2d.worker.processor.eob;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.worker.TestUtil;
import gov.cms.ab2d.worker.processor.StreamHelper;
import gov.cms.ab2d.worker.processor.TextStreamHelperImpl;
import gov.cms.ab2d.worker.service.FileService;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
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

import static gov.cms.ab2d.worker.processor.eob.BundleUtils.createIdentifierWithoutMbi;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractProcessorInvalidPatientTest {
    @Mock
    private FileService fileService;
    @Mock
    private PatientClaimsProcessor patientClaimsProcessor;
    @Mock
    private LogManager eventLogger;
    @Mock
    private BFDClient bfdClient;
    @Mock
    private JobRepository jobRepository;

    @TempDir
    File tmpDirFolder;

    private ContractProcessor cut;
    private ContractData contractData;
    private ProgressTracker tracker;
    private FhirContext fhirContext = FhirContext.forDstu3();
    private String jobId = "1234";
    private String contractId = "ABC";

    @BeforeEach
    void setup() {

        patientClaimsProcessor = new PatientClaimsProcessorImpl(bfdClient, eventLogger);
        cut = new ContractProcessorImpl(fileService, jobRepository, patientClaimsProcessor, eventLogger, fhirContext);
        tracker = ProgressTracker.builder()
                .jobUuid(jobId)
                .failureThreshold(100)
                .build();

        Contract contract = new Contract();
        contract.setContractNumber(contractId);
        contract.setAttestedOn(OffsetDateTime.now().minusYears(50));
        contractData = new ContractData(contract, tracker, OffsetDateTime.MIN, "User");

        List<FilterOutByDate.DateRange> dates = singletonList(TestUtil.getOpenRange());
        List<CoverageSummary> summaries = List.of(
                new CoverageSummary(createIdentifierWithoutMbi("1"), null, dates),
                new CoverageSummary(createIdentifierWithoutMbi("2"), null, dates),
                new CoverageSummary(createIdentifierWithoutMbi("3"), null, dates)
        );

        tracker.addPatients(summaries);

        ReflectionTestUtils.setField(cut, "cancellationCheckFrequency", 20);
        ReflectionTestUtils.setField(patientClaimsProcessor, "startDate", "01/01/2020");
    }

    @Test
    void testInvalidBenes() throws IOException {
        Bundle b1 = BundleUtils.createBundle(createBundleEntry("1"));
        Bundle b2 = BundleUtils.createBundle(createBundleEntry("2"));
        Bundle b4 = BundleUtils.createBundle(createBundleEntry("4"));
        when(bfdClient.requestEOBFromServer(eq("1"), any())).thenReturn(b1);
        when(bfdClient.requestEOBFromServer(eq("2"), any())).thenReturn(b2);
        when(bfdClient.requestEOBFromServer(eq("3"), any())).thenReturn(b4);
        List<JobOutput> outputs = cut.process(tmpDirFolder.toPath(), contractData);
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
        String val = "Hello World";
        StreamHelper helper = new TextStreamHelperImpl(
                tmpDirFolder.toPath(), contractId, 2000, 10, eventLogger, job);
        ((ContractProcessorImpl) cut).writeExceptionToContractErrorFile(
                helper, val, new RuntimeException("Exception"));
        String result = Files.readString(Path.of(tmpDirFolder.getAbsolutePath() + File.separator + contractId + "_error.ndjson"));
        assertEquals(val, result);
    }

    @Test
    void testWriteNullErrors() throws IOException {
        Job job = new Job();
        job.setJobUuid(jobId);
        StreamHelper helper = new TextStreamHelperImpl(
                tmpDirFolder.toPath(), contractId, 2000, 10, eventLogger, job);
        ((ContractProcessorImpl) cut).writeExceptionToContractErrorFile(
                helper, null, new RuntimeException("Exception"));
        assertThrows(NoSuchFileException.class, () -> Files.readString(Path.of(tmpDirFolder.getAbsolutePath() + File.separator + contractId + "_error.ndjson")));
    }

    private static ExplanationOfBenefit createEOB(String patientId) {
        ExplanationOfBenefit b = new ExplanationOfBenefit();
        Period p = new Period();
        p.setStart(new Date(0));
        p.setEnd(new Date());
        b.setBillablePeriod(p);
        Reference ref = new Reference("Patient/" + patientId);
        b.setPatient(ref);
        return b;
    }

    public static Bundle.BundleEntryComponent createBundleEntry(String patientId) {
        var component = new Bundle.BundleEntryComponent();
        component.setResource(createEOB(patientId));
        return component;
    }
}
