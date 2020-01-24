package gov.cms.ab2d.worker.adapter.bluebutton;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmer;
import gov.cms.ab2d.worker.service.FileService;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class PatientClaimsProcessorUnitTest {
    // class under test
    private PatientClaimsProcessor cut;

    @Mock private BFDClient mockBfdClient;
    @Mock private FileService mockFileService;

    @TempDir
    File tmpEfsMountDir;

    private ExplanationOfBenefit eob;
    private Path outputFile;
    private Path errorFile;
    private String patientId = "1234567890";

    private List<Integer> allMonths = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    private OffsetDateTime earlyAttDate = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private GetPatientsByContractResponse.PatientDTO patientDTO;

    @BeforeEach
    void setUp() throws IOException {
        FhirContext fhirContext = ca.uhn.fhir.context.FhirContext.forDstu3();
        cut = new PatientClaimsProcessorImpl(
                mockBfdClient,
                fhirContext,
                mockFileService
        );
        ReflectionTestUtils.setField(cut, "tryLockTimeout", 30);

        createEOB();
        createOutputFiles();
        patientDTO = new GetPatientsByContractResponse.PatientDTO();
        patientDTO.setPatientId(patientId);
        patientDTO.setMonthsUnderContract(allMonths);
    }


    @Test
    void process_whenPatientHasSinglePageOfClaimsData() throws IOException, ExecutionException, InterruptedException {
        Bundle bundle1 = createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(patientId)).thenReturn(bundle1);

        cut.process(patientDTO, new ReentrantLock(), outputFile, errorFile, earlyAttDate).get();

        verify(mockBfdClient).requestEOBFromServer(patientId);
        verify(mockBfdClient, never()).requestNextBundleFromServer(bundle1);
        verify(mockFileService).appendToFile(any(), any());
    }

    @Test
    void process_whenPatientHasMultiplePagesOfClaimsData() throws IOException, ExecutionException, InterruptedException {
        Bundle bundle1 = createBundle(eob.copy());
        bundle1.addLink(addNextLink());

        Bundle bundle2 = createBundle(eob.copy());

        when(mockBfdClient.requestEOBFromServer(patientId)).thenReturn(bundle1);
        when(mockBfdClient.requestNextBundleFromServer(bundle1)).thenReturn(bundle2);

        cut.process(patientDTO, new ReentrantLock(), outputFile, errorFile, earlyAttDate).get();

        verify(mockBfdClient).requestEOBFromServer(patientId);
        verify(mockBfdClient).requestNextBundleFromServer(bundle1);
        verify(mockFileService).appendToFile(any(), any());
    }


    @Test
    void process_whenBfdClientThrowsException() throws IOException {
        Bundle bundle1 = createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(patientId)).thenThrow(new RuntimeException("Test Exception"));

        var exceptionThrown = assertThrows(RuntimeException.class,
                () -> cut.process(patientDTO, new ReentrantLock(), outputFile, errorFile, earlyAttDate).get());

        assertThat(exceptionThrown.getCause().getMessage(), startsWith("Test Exception"));

        verify(mockBfdClient).requestEOBFromServer(patientId);
        verify(mockBfdClient, never()).requestNextBundleFromServer(bundle1);
        verify(mockFileService).appendToFile(any(), any());
    }

    @Test
    void process_whenPatientHasNoEOBClaimsData() throws IOException, ExecutionException, InterruptedException {
        Bundle bundle1 = new Bundle();
        when(mockBfdClient.requestEOBFromServer(patientId)).thenReturn(bundle1);

        cut.process(patientDTO, new ReentrantLock(), outputFile, errorFile, earlyAttDate).get();

        verify(mockBfdClient).requestEOBFromServer(patientId);
        verify(mockBfdClient, never()).requestNextBundleFromServer(bundle1);
        verify(mockFileService, never()).appendToFile(any(), any());
    }


    private void createEOB() {
        final String testInputFile = "test-data/EOB-for-Carrier-Claims.json";
        final InputStream inputStream = getClass().getResourceAsStream("/" + testInputFile);

        final EncodingEnum respType = EncodingEnum.forContentType(EncodingEnum.JSON_PLAIN_STRING);
        final IParser parser = respType.newParser(FhirContext.forDstu3());
        final ExplanationOfBenefit explanationOfBenefit = parser.parseResource(ExplanationOfBenefit.class, inputStream);
        eob = ExplanationOfBenefitTrimmer.getBenefit(explanationOfBenefit);
    }

    private void createOutputFiles() throws IOException {
        final Path outputDirPath = Paths.get(tmpEfsMountDir.toPath().toString(), UUID.randomUUID().toString());
        Files.createDirectories(outputDirPath);

        outputFile = createFile(outputDirPath, "contract_name.ndjson");
        errorFile = createFile(outputDirPath, "contract_name_error.ndjson");
    }

    private Path createFile(Path outputDirPath, String output_filename) throws IOException {
        final Path outputFilePath = Path.of(outputDirPath.toString(), output_filename);
        return Files.createFile(outputFilePath);
    }

    private Bundle createBundle(Resource resource) {
        final Bundle bundle = new Bundle();
        bundle.addEntry(addEntry(resource));
        return bundle;
    }

    private Bundle.BundleEntryComponent addEntry(Resource resource) {
        final Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
        bundleEntryComponent.setResource(resource);
        return bundleEntryComponent;
    }


    private Bundle.BundleLinkComponent addNextLink() {
        Bundle.BundleLinkComponent linkComponent = new Bundle.BundleLinkComponent();
        linkComponent.setRelation(Bundle.LINK_NEXT);
        return linkComponent;
    }


}
