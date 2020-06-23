package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.context.FhirContext;
import com.newrelic.api.agent.Token;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class PatientClaimsProcessorUnitTest {
    // class under test
    private PatientClaimsProcessor cut;

    @Mock private BFDClient mockBfdClient;
    @Mock private LogManager eventLogger;

    @TempDir
    File tmpEfsMountDir;

    private ExplanationOfBenefit eob;
    private String patientId = "1234567890";

    private OffsetDateTime earlyAttDate = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private GetPatientsByContractResponse.PatientDTO patientDTO;

    private Token noOpToken =  new Token() {
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
        FhirContext fhirContext = ca.uhn.fhir.context.FhirContext.forDstu3();
        cut = new PatientClaimsProcessorImpl(
                mockBfdClient,
                fhirContext,
                eventLogger
        );

        eob = EobTestDataUtil.createEOB();
        createOutputFiles();
        patientDTO = new GetPatientsByContractResponse.PatientDTO();
        patientDTO.setPatientId(patientId);
        patientDTO.setDateRangesUnderContract(List.of(new FilterOutByDate.DateRange(new Date(0), new Date())));

        Contract contract = new Contract();
        StreamHelper helper = new TextStreamHelperImpl(tmpEfsMountDir.toPath(), contract.getContractNumber(),
                30, 120, eventLogger, null);

        request = new PatientClaimsRequest(patientDTO, helper, earlyAttDate, null, "user", "job",
                "contractNum", noOpToken);
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

        assertThat(exceptionThrown.getCause().getMessage(), startsWith("Test Exception"));

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
        patientDTO = new GetPatientsByContractResponse.PatientDTO();
        patientDTO.setPatientId(patientId);
        patientDTO.setDateRangesUnderContract(List.of(new FilterOutByDate.DateRange(new Date(0), new Date())));

        Contract contract = new Contract();
        StreamHelper helper = new TextStreamHelperImpl(tmpEfsMountDir.toPath(), contract.getContractNumber(),
                30, 120, eventLogger, null);

        OffsetDateTime sinceDate = earlyAttDate.plusDays(1);

        request = new PatientClaimsRequest(patientDTO, helper, earlyAttDate, sinceDate, "user", "job",
                "contractNum", noOpToken);

        Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(patientId, request.getSinceTime())).thenReturn(bundle1);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(patientId, request.getSinceTime());
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
