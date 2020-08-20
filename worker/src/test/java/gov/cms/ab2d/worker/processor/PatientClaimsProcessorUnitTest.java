package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.Token;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.filter.FilterOutByDate.DateRange;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.processor.domainmodel.EobSearchResponse;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private OffsetDateTime earlyDate;

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
        ReflectionTestUtils.setField(cut, "startDate", "01/01/1990");
        ReflectionTestUtils.setField(cut, "startDateSpecialContracts", "Z0001,S0001");

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
        earlyDate = OffsetDateTime.of(1990, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        request = new PatientClaimsRequest(patientDTO, earlyDate, earlyDate, null, "job", "contractNum", noOpToken);

    }

    @Test
    void process_whenPatientHasSinglePageOfClaimsData() throws ExecutionException, InterruptedException {
        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(patientId, earlyDate)).thenReturn(bundle1);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(patientId, earlyDate);
        verify(mockBfdClient, never()).requestNextBundleFromServer(bundle1);
    }

    @Test
    void process_whenPatientHasMultiplePagesOfClaimsData() throws ExecutionException, InterruptedException {
        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        bundle1.addLink(EobTestDataUtil.addNextLink());

        Bundle bundle2 = EobTestDataUtil.createBundle(eob.copy());

        when(mockBfdClient.requestEOBFromServer(patientId, earlyDate)).thenReturn(bundle1);
        when(mockBfdClient.requestNextBundleFromServer(bundle1)).thenReturn(bundle2);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(patientId, earlyDate);
        verify(mockBfdClient).requestNextBundleFromServer(bundle1);
    }

    @Test
    void process_whenBfdClientThrowsException() {
        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(patientId, earlyDate)).thenThrow(new RuntimeException("Test Exception"));

        var exceptionThrown = assertThrows(RuntimeException.class,
                () -> cut.process(request).get());

        assertTrue(exceptionThrown.getMessage().startsWith("Test Exception"));

        verify(mockBfdClient).requestEOBFromServer(patientId, earlyDate);
        verify(mockBfdClient, never()).requestNextBundleFromServer(bundle1);
    }

    @Test
    void process_whenPatientHasNoEOBClaimsData() throws ExecutionException, InterruptedException {
        Bundle bundle1 = new Bundle();
        when(mockBfdClient.requestEOBFromServer(any(), any())).thenReturn(bundle1);

        EobSearchResponse response = cut.process(request).get();
        assertNotNull(response);
        assertNotNull(response.getResources());
        assertEquals(0, response.getResources().size());
        verify(mockBfdClient).requestEOBFromServer(any(), any());
    }

    @Test
    void process_whenPatientHasSinglePageOfClaimsDataSince() throws ExecutionException, InterruptedException,
            ParseException {
        // Override default behavior of setup
        patientDTO = new ContractBeneficiaries.PatientDTO();
        patientDTO.setPatientId(patientId);
        patientDTO.setDateRangesUnderContract(List.of(new DateRange(new Date(0), new Date())));

        request = new PatientClaimsRequest(patientDTO, earlyDate, earlyDate, "user", "job", "contractNum", noOpToken);
        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(patientId, earlyDate)).thenReturn(bundle1);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(patientId, earlyDate);
        verify(mockBfdClient, never()).requestNextBundleFromServer(bundle1);
    }

    @Test
    void process_whenPatientHasSinglePageOfClaimsDataEarlyAttDate() throws ExecutionException, InterruptedException,
            ParseException {
        // Override default behavior of setup
        patientDTO = new ContractBeneficiaries.PatientDTO();
        patientDTO.setPatientId(patientId);
        patientDTO.setDateRangesUnderContract(List.of(new DateRange(new Date(0), new Date())));

        request = new PatientClaimsRequest(patientDTO, earlyDate, earlyDate, "user", "job",
                "contractNum", noOpToken);

        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(patientId, earlyDate)).thenReturn(bundle1);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(patientId, earlyDate);
        verify(mockBfdClient, never()).requestNextBundleFromServer(bundle1);
    }


    private void createOutputFiles() throws IOException {
        final Path outputDirPath = Paths.get(tmpEfsMountDir.toPath().toString(), UUID.randomUUID().toString());
        Files.createDirectories(outputDirPath);

        Path outputFile = createFile(outputDirPath, "contract_name.ndjson");
        assertNotNull(outputFile);
        Path errorFile = createFile(outputDirPath, "contract_name_error.ndjson");
        assertNotNull(errorFile);
    }

    private Path createFile(Path outputDirPath, String output_filename) throws IOException {
        final Path outputFilePath = Path.of(outputDirPath.toString(), output_filename);
        return Files.createFile(outputFilePath);
    }
}
