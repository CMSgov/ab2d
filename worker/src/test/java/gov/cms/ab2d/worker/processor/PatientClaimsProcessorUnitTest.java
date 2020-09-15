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

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private final static String patientId = "1234567890";
    private final static String SAMPLE_CONTRACT_ID = "CONTRACT1";

    private final OffsetDateTime earlyAttDate = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime laterAttDate = OffsetDateTime.of(2020, 2, 15, 0, 0, 0, 0, ZoneOffset.UTC);
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

        request = new PatientClaimsRequest(patientDTO, laterAttDate, null, "user", "job",
                "contractNum", noOpToken);
    }

    @Test
    void process_whenContractIsSpecialContract() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        Date d1 = sdf.parse("01/02/2020");
        Date d2 = sdf.parse("12/02/2019");
        ReflectionTestUtils.setField(cut, "startDate", "01/01/2020");
        ReflectionTestUtils.setField(cut, "startDateSpecialContracts", "01/01/2019");
        ReflectionTestUtils.setField(cut, "specialContracts", List.of("Z0001"));

        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        ExplanationOfBenefit eob = (ExplanationOfBenefit) bundle1.getEntry().get(0).getResource();

        eob.getBillablePeriod().setStart(d1);
        eob.getBillablePeriod().setEnd(d1);
        List<ExplanationOfBenefit> resources = cut.extractResources(SAMPLE_CONTRACT_ID, bundle1.getEntry(),
                List.of(new DateRange(new Date(0), new Date())), OffsetDateTime.now().minusYears(10));
        assertEquals(1, resources.size());

        resources = cut.extractResources("Z0001", bundle1.getEntry(),
                List.of(new DateRange(new Date(0), new Date())), OffsetDateTime.now().minusYears(10));
        assertEquals(1, resources.size());

        eob.getBillablePeriod().setStart(d2);
        eob.getBillablePeriod().setEnd(d2);
        resources = cut.extractResources(SAMPLE_CONTRACT_ID, bundle1.getEntry(),
                List.of(new DateRange(new Date(0), new Date())), OffsetDateTime.now().minusYears(10));
        assertEquals(0, resources.size());
        resources = cut.extractResources("Z0001", bundle1.getEntry(),
                List.of(new DateRange(new Date(0), new Date())), OffsetDateTime.now().minusYears(10));
        assertEquals(1, resources.size());
    }

    @Test
    void process_whenPDPhasAttestedBeforeBeginDate() throws ParseException {
        // Set the earliest date to Jan 1
        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        ReflectionTestUtils.setField(cut, "startDate", "01/01/2020");
        // Attestation time is 10 years ago, eob date is 01/02/2020
        List<ExplanationOfBenefit> resources = cut.extractResources(SAMPLE_CONTRACT_ID, bundle1.getEntry(),
                List.of(new DateRange(new Date(0), new Date())), OffsetDateTime.now().minusYears(10));
        assertEquals(1, resources.size());
        // Set the billable date to 1970 and attestation date to 1920, should return no results
        ExplanationOfBenefit eob = (ExplanationOfBenefit) bundle1.getEntry().get(0).getResource();
        eob.getBillablePeriod().setStart(new Date(10));
        eob.getBillablePeriod().setEnd(new Date(10));
        resources = cut.extractResources(SAMPLE_CONTRACT_ID, bundle1.getEntry(),
                List.of(new DateRange(new Date(0), new Date())), OffsetDateTime.now().minusYears(100));
        assertEquals(0, resources.size());
        // Set billable date to late year and attestation date to a hundred years ago, shouldn't return results
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        eob.getBillablePeriod().setStart(sdf.parse("12/29/2019"));
        eob.getBillablePeriod().setEnd(sdf.parse("12/30/2019"));
        resources = cut.extractResources(SAMPLE_CONTRACT_ID, bundle1.getEntry(),
                List.of(new DateRange(new Date(0), new Date())), OffsetDateTime.now().minusYears(100));
        assertEquals(0, resources.size());
        // Set billable period to early 2020, attestation date in 2019, should return 1
        eob.getBillablePeriod().setStart(sdf.parse("01/02/2020"));
        eob.getBillablePeriod().setEnd(sdf.parse("01/03/2020"));
        resources = cut.extractResources(SAMPLE_CONTRACT_ID, bundle1.getEntry(),
                List.of(new DateRange(new Date(0), new Date())), OffsetDateTime.of(2019, 1, 1,
                        1, 1, 1, 1, ZoneOffset.UTC));
        assertEquals(1, resources.size());
        // billable period is early 2020, attestation date is today, should return 0
        resources = cut.extractResources(SAMPLE_CONTRACT_ID, bundle1.getEntry(),
                List.of(new DateRange(new Date(0), new Date())), OffsetDateTime.now());
        assertEquals(0, resources.size());
    }

    @Test
    void process_whenPatientHasSinglePageOfClaimsData() throws ExecutionException, InterruptedException {
        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(patientId, request.getAttTime())).thenReturn(bundle1);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(patientId, request.getAttTime());
        verify(mockBfdClient, never()).requestNextBundleFromServer(bundle1);
    }

    @Test
    void process_whenPatientHasMultiplePagesOfClaimsData() throws ExecutionException, InterruptedException {
        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        bundle1.addLink(EobTestDataUtil.addNextLink());

        Bundle bundle2 = EobTestDataUtil.createBundle(eob.copy());

        when(mockBfdClient.requestEOBFromServer(patientId, request.getAttTime())).thenReturn(bundle1);
        when(mockBfdClient.requestNextBundleFromServer(bundle1)).thenReturn(bundle2);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(patientId, request.getAttTime());
        verify(mockBfdClient).requestNextBundleFromServer(bundle1);
    }

    @Test
    void process_whenBfdClientThrowsException() {
        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(patientId, request.getAttTime())).thenThrow(new RuntimeException("Test Exception"));

        var exceptionThrown = assertThrows(ExecutionException.class,
                () -> cut.process(request).get());

        assertTrue(exceptionThrown.getCause().getMessage().startsWith("Test Exception"));

        verify(mockBfdClient).requestEOBFromServer(patientId, request.getAttTime());
        verify(mockBfdClient, never()).requestNextBundleFromServer(bundle1);
    }

    @Test
    void process_whenPatientHasNoEOBClaimsData() throws ExecutionException, InterruptedException {
        Bundle bundle1 = new Bundle();
        when(mockBfdClient.requestEOBFromServer(patientId, request.getAttTime())).thenReturn(bundle1);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(patientId, request.getAttTime());
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

        request = new PatientClaimsRequest(patientDTO, laterAttDate, sinceDate, "user", "job",
                "contractNum", noOpToken);

        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(patientId, request.getSinceTime())).thenReturn(bundle1);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(patientId, request.getSinceTime());
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

        request = new PatientClaimsRequest(patientDTO, earlyAttDate, null, "user", "job",
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

        createFile(outputDirPath, "contract_name.ndjson");
        createFile(outputDirPath, "contract_name_error.ndjson");
    }

    private Path createFile(Path outputDirPath, String output_filename) throws IOException {
        final Path outputFilePath = Path.of(outputDirPath.toString(), output_filename);
        return Files.createFile(outputFilePath);
    }
}
