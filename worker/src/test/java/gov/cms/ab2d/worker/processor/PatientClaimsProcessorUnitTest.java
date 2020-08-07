package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.Token;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.filter.FilterOutByDate.DateRange;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.processor.domainmodel.PatientClaimsRequest;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class PatientClaimsProcessorUnitTest {
    // class under test
    private PatientClaimsProcessorImpl cut;

    @Mock private BFDClient mockBfdClient;
    @Mock private LogManager eventLogger;

    @TempDir
    File tmpEfsMountDir;

    private ExplanationOfBenefit eob;
    private Map<String, ContractBeneficiaries.PatientDTO> patientPTOMap;
    private String patientId = "1234567890";

    private OffsetDateTime earlyAttDate = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private ContractBeneficiaries.PatientDTO patientDTO;

    private final Token noOpToken = new Token() {
        @Override
        public boolean link() {
            return false;
        }

        @Override
        public boolean expire() {
            return false;
        }

        @Override
        public boolean linkAndExpire() {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }
    };
    private PatientClaimsRequest request;

    @BeforeEach
    void setUp() throws Exception {
        cut = new PatientClaimsProcessorImpl(
                mockBfdClient,
                eventLogger
        );

        ReflectionTestUtils.setField(cut, "startDate", "01/01/1900");

        eob = EobTestDataUtil.createEOB();
        createOutputFiles();
        patientDTO = new ContractBeneficiaries.PatientDTO();
        patientDTO.setPatientId(patientId);
        patientDTO.setDateRangesUnderContract(List.of(new DateRange(new Date(0), new Date())));

        patientPTOMap = new HashMap<>();
        patientPTOMap.put(patientId, patientDTO);
        ContractBeneficiaries.PatientDTO fileDTO = new ContractBeneficiaries.PatientDTO();
        fileDTO.setPatientId("-199900000022040");
        fileDTO.setDateRangesUnderContract(Collections.singletonList(new DateRange(new Date(0), new Date())));
        patientPTOMap.put(fileDTO.getPatientId(), fileDTO);

        Contract contract = new Contract();
        StreamHelper helper = new TextStreamHelperImpl(tmpEfsMountDir.toPath(), contract.getContractNumber(),
                30, 120, eventLogger, null);

        request = new PatientClaimsRequest(patientDTO, null, "job", "contractNum", noOpToken);
    }

    @Test
    void process_whenPatientInEOBIsNotInContract() throws ParseException {
        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        ExplanationOfBenefit eob = (ExplanationOfBenefit) bundle1.getEntry().get(0).getResource();
        eob.getPatient().setReference("Patient/BADPATID");
        List<Resource> resources = cut.extractResources(bundle1.getEntry());
        assertEquals(resources.size(), 0);
    }

    @Test
    void process_whenPatientInEOBIsNotInContract2() throws ParseException {
        // Empty the patient list
        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        patientPTOMap.clear();
        List<Resource> resources = cut.extractResources(bundle1.getEntry());
        assertEquals(resources.size(), 0);

        // Enter an invalid patient list
        ContractBeneficiaries.PatientDTO badPatient = new ContractBeneficiaries.PatientDTO();
        badPatient.setPatientId("BADPATID");
        badPatient.setDateRangesUnderContract(List.of(new DateRange(new Date(0), new Date())));
        patientPTOMap.put("BADPATID", badPatient);
        resources = cut.extractResources(bundle1.getEntry());
        assertEquals(resources.size(), 0);

        // Add a correct patient and verify it comes over
        ContractBeneficiaries.PatientDTO goodPatient = new ContractBeneficiaries.PatientDTO();
        String goodPatientId = "-199900000022040";
        goodPatient.setPatientId(goodPatientId);
        goodPatient.setDateRangesUnderContract(List.of(new DateRange(new Date(0), new Date())));
        assertEquals(resources.size(), 0);
        patientPTOMap.put(goodPatientId, goodPatient);
        resources = cut.extractResources(bundle1.getEntry());
        assertEquals(resources.size(), 1);
    }

    @Test
    void process_whenPDPhasAttestedBeforeBeginDate() throws ParseException {
        // Set the earliest date to Jan 1
        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        ReflectionTestUtils.setField(cut, "startDate", "01/01/2020");
        // Attestation time is 10 years ago, eob date is 01/02/2020
        List<Resource> resources = cut.extractResources(bundle1.getEntry());
        assertEquals(1, resources.size());
        // Set the billable date to 1970 and attestation date to 1920, should return no results
        ExplanationOfBenefit eob = (ExplanationOfBenefit) bundle1.getEntry().get(0).getResource();
        eob.getBillablePeriod().setStart(new Date(10));
        eob.getBillablePeriod().setEnd(new Date(10));
        resources = cut.extractResources(bundle1.getEntry());
        assertEquals(0, resources.size());
        // Set billable date to late year and attestation date to a hundred years ago, shouldn't return results
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        eob.getBillablePeriod().setStart(sdf.parse("12/29/2019"));
        eob.getBillablePeriod().setEnd(sdf.parse("12/30/2019"));
        resources = cut.extractResources(bundle1.getEntry());
        assertEquals(0, resources.size());
        // Set billable period to early 2020, attestation date in 2019, should return 1
        eob.getBillablePeriod().setStart(sdf.parse("01/02/2020"));
        eob.getBillablePeriod().setEnd(sdf.parse("01/03/2020"));
        resources = cut.extractResources(bundle1.getEntry());
        assertEquals(1, resources.size());
        // billable period is early 2020, attestation date is today, should return 0
        resources = cut.extractResources(bundle1.getEntry());
        assertEquals(0, resources.size());
    }

    @Test
    void process_whenPatientHasSinglePageOfClaimsData() throws ExecutionException, InterruptedException {
        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(patientId)).thenReturn(bundle1);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(patientId);
        verify(mockBfdClient, never()).requestNextBundleFromServer(bundle1);
    }

    @Test
    void process_whenPatientHasMultiplePagesOfClaimsData() throws ExecutionException, InterruptedException {
        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        bundle1.addLink(EobTestDataUtil.addNextLink());

        Bundle bundle2 = EobTestDataUtil.createBundle(eob.copy());

        when(mockBfdClient.requestEOBFromServer(patientId, OffsetDateTime.MIN)).thenReturn(bundle1);
        when(mockBfdClient.requestNextBundleFromServer(bundle1)).thenReturn(bundle2);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(patientId, OffsetDateTime.MIN);
        verify(mockBfdClient).requestNextBundleFromServer(bundle1);
    }

    @Test
    void process_whenBfdClientThrowsException() {
        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(patientId, OffsetDateTime.MIN)).thenThrow(new RuntimeException("Test Exception"));

        var exceptionThrown = assertThrows(ExecutionException.class,
                () -> cut.process(request).get());

        assertThat(exceptionThrown.getCause().getMessage(), startsWith("Test Exception"));

        verify(mockBfdClient).requestEOBFromServer(patientId, OffsetDateTime.MIN);
        verify(mockBfdClient, never()).requestNextBundleFromServer(bundle1);
    }

    @Test
    void process_whenPatientHasNoEOBClaimsData() throws ExecutionException, InterruptedException {
        Bundle bundle1 = new Bundle();
        when(mockBfdClient.requestEOBFromServer(patientId, OffsetDateTime.MIN)).thenReturn(bundle1);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(patientId, OffsetDateTime.MIN);
        verify(mockBfdClient, never()).requestNextBundleFromServer(bundle1);
    }

    @Test
    void process_whenPatientHasSinglePageOfClaimsDataSince() throws ExecutionException, InterruptedException,
            FileNotFoundException, ParseException {
        // Override default behavior of setup
        patientDTO = new ContractBeneficiaries.PatientDTO();
        patientDTO.setPatientId(patientId);
        patientDTO.setDateRangesUnderContract(List.of(new DateRange(new Date(0), new Date())));

        Contract contract = new Contract();
        StreamHelper helper = new TextStreamHelperImpl(tmpEfsMountDir.toPath(), contract.getContractNumber(),
                30, 120, eventLogger, null);

        OffsetDateTime sinceDate = earlyAttDate.plusDays(1);

        request = new PatientClaimsRequest(patientDTO, "user", "job", "contractNum", noOpToken);
         Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(patientId, OffsetDateTime.MIN)).thenReturn(bundle1);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(patientId, OffsetDateTime.MIN);
        verify(mockBfdClient, never()).requestNextBundleFromServer(bundle1);
    }

    @Test
    void process_whenPatientHasSinglePageOfClaimsDataEarlyAttDate() throws ExecutionException, InterruptedException,
            FileNotFoundException, ParseException {
        // Override default behavior of setup
        patientDTO = new ContractBeneficiaries.PatientDTO();
        patientDTO.setPatientId(patientId);
        patientDTO.setDateRangesUnderContract(List.of(new DateRange(new Date(0), new Date())));

        Contract contract = new Contract();
        StreamHelper helper = new TextStreamHelperImpl(tmpEfsMountDir.toPath(), contract.getContractNumber(),
                30, 120, eventLogger, null);

        request = new PatientClaimsRequest(patientDTO, "user", "job",
                "contractNum", noOpToken);

        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(patientId, null)).thenReturn(bundle1);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(patientId, null);
        verify(mockBfdClient, never()).requestNextBundleFromServer(bundle1);
    }


    private void createOutputFiles() throws IOException {
        final Path outputDirPath = Paths.get(tmpEfsMountDir.toPath().toString(), UUID.randomUUID().toString());
        Files.createDirectories(outputDirPath);

        Path outputFile = createFile(outputDirPath, "contract_name.ndjson");
        Path errorFile = createFile(outputDirPath, "contract_name_error.ndjson");
    }

    private Path createFile(Path outputDirPath, String output_filename) throws IOException {
        final Path outputFilePath = Path.of(outputDirPath.toString(), output_filename);
        return Files.createFile(outputFilePath);
    }
}
