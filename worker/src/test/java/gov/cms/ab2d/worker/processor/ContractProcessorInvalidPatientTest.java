package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.processor.domainmodel.ContractData;
import gov.cms.ab2d.worker.processor.domainmodel.ProgressTracker;
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
import java.nio.file.Path;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ContractProcessorInvalidPatientTest {
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

    @BeforeEach
    void setup() throws ParseException {

        String jobId = "1234";
        String contractId = "ABC";
        patientClaimsProcessor = new PatientClaimsProcessorImpl(bfdClient, eventLogger);
        cut = new ContractProcessorImpl(fileService, jobRepository, patientClaimsProcessor, eventLogger, fhirContext);
        tracker = ProgressTracker.builder()
                .currentMonth(9)
                .jobUuid(jobId)
                .numContracts(1)
                .failureThreshold(5)
                .build();

        Contract contract = new Contract();
        contract.setContractNumber("ABC");
        contract.setAttestedOn(OffsetDateTime.now().minusYears(50));
        contractData = new ContractData(contract, tracker, OffsetDateTime.MIN, "User");
        Bundle b1 = BundleUtils.createBundle(createBundleEntry("1"));
        Bundle b2 = BundleUtils.createBundle(createBundleEntry("2"));
        Bundle b4 = BundleUtils.createBundle(createBundleEntry("4"));
        ContractBeneficiaries cb = new ContractBeneficiaries();
        cb.setContractNumber(contractId);
        Map<String, ContractBeneficiaries.PatientDTO> map = new HashMap<>();
        cb.setPatients(map);
        List<FilterOutByDate.DateRange> dates = Collections.singletonList(new FilterOutByDate.DateRange(new Date(0), new Date()));
        map.put("1", ContractBeneficiaries.PatientDTO.builder().patientId("1").dateRangesUnderContract(dates).build());
        map.put("2", ContractBeneficiaries.PatientDTO.builder().patientId("2").dateRangesUnderContract(dates).build());
        map.put("3", ContractBeneficiaries.PatientDTO.builder().patientId("3").dateRangesUnderContract(dates).build());
        tracker.addPatientsByContract(cb);
        when(bfdClient.requestEOBFromServer(eq("1"), any())).thenReturn(b1);
        when(bfdClient.requestEOBFromServer(eq("2"), any())).thenReturn(b2);
        when(bfdClient.requestEOBFromServer(eq("3"), any())).thenReturn(b4);
        ReflectionTestUtils.setField(cut, "cancellationCheckFrequency", 20);
        ReflectionTestUtils.setField(patientClaimsProcessor, "startDate", "01/01/2020");
    }

    @Test
    void testInvalidBenes() throws IOException {
        List<JobOutput> outputs = cut.process(tmpDirFolder.toPath(), contractData);
        assertNotNull(outputs);
        assertEquals(2, outputs.size());
        String fileName1 = "ABC_0001.ndjson";
        String fileName2 = "ABC_0002.ndjson";
        String output1 = outputs.get(0).getFilePath();
        String output2 = outputs.get(1).getFilePath();
        assertTrue(output1.equalsIgnoreCase(fileName1) || output1.equalsIgnoreCase(fileName2));
        assertTrue(output2.equalsIgnoreCase(fileName1) || output2.equalsIgnoreCase(fileName2));
        String actual1 = Files.readString(Path.of(tmpDirFolder.getAbsolutePath() + "/" + output1));
        String actual2 = Files.readString(Path.of(tmpDirFolder.getAbsolutePath() + "/" + output2));
        assertTrue(actual1.contains("Patient/1") || actual1.contains("Patient/2"));
        assertTrue(actual2.contains("Patient/1") || actual2.contains("Patient/2"));
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
