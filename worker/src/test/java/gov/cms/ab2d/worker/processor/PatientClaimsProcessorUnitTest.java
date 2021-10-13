package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.Token;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.worker.TestUtil;
import lombok.extern.slf4j.Slf4j;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifierWithoutMbi;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class PatientClaimsProcessorUnitTest {
    // class under test
    private PatientClaimsProcessorImpl cut;

    @Mock private BFDClient mockBfdClient;
    @Mock private LogManager eventLogger;

    @TempDir
    File tmpEfsMountDir;

    private org.hl7.fhir.dstu3.model.ExplanationOfBenefit eob;
    private final static Long patientId = -199900000022040L;

    private static final OffsetDateTime EARLY_ATT_DATE = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime EARLY_SINCE_DATE = OffsetDateTime.of(2020, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime LATER_ATT_DATE = OffsetDateTime.of(2020, 2, 15, 0, 0, 0, 0, ZoneOffset.UTC);
    private CoverageSummary coverageSummary;

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

        ReflectionTestUtils.setField(cut, "earliestDataDate", "01/01/1900");

        eob = (ExplanationOfBenefit) EobTestDataUtil.createEOB();
        createOutputFiles();

        List<CoverageSummary> coverageSummaries = new ArrayList<>();

        coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(patientId),
                null, List.of(TestUtil.getOpenRange()));
        coverageSummaries.add(coverageSummary);

        CoverageSummary fileSummary = new CoverageSummary(createIdentifierWithoutMbi(-199900000022040L),
                null, List.of(TestUtil.getOpenRange()));
        coverageSummaries.add(fileSummary);

        request = new PatientClaimsRequest(coverageSummary, LATER_ATT_DATE, null, "client", "job",
                "contractNum", noOpToken, STU3);
    }

    @Test
    void process_whenPatientHasSinglePageOfClaimsData() throws ExecutionException, InterruptedException {
        org.hl7.fhir.dstu3.model.Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(STU3, patientId, request.getAttTime())).thenReturn(bundle1);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(STU3, patientId, request.getAttTime());
        verify(mockBfdClient, never()).requestNextBundleFromServer(STU3, bundle1);
    }

    @Test
    void process_whenPatientHasMultiplePagesOfClaimsData() throws ExecutionException, InterruptedException {
        org.hl7.fhir.dstu3.model.Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        bundle1.addLink(EobTestDataUtil.addNextLink());

        org.hl7.fhir.dstu3.model.Bundle bundle2 = EobTestDataUtil.createBundle(eob.copy());

        when(mockBfdClient.requestEOBFromServer(STU3, patientId, request.getAttTime())).thenReturn(bundle1);
        when(mockBfdClient.requestNextBundleFromServer(STU3, bundle1)).thenReturn(bundle2);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(STU3, patientId, request.getAttTime());
        verify(mockBfdClient).requestNextBundleFromServer(STU3, bundle1);
    }

    @Test
    void process_whenBfdClientThrowsException() {
        org.hl7.fhir.dstu3.model.Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(STU3, patientId, request.getAttTime())).thenThrow(new RuntimeException("Test Exception"));

        var exceptionThrown = assertThrows(ExecutionException.class,
                () -> cut.process(request).get());

        assertTrue(exceptionThrown.getCause().getMessage().startsWith("Test Exception"));

        verify(mockBfdClient).requestEOBFromServer(STU3, patientId, request.getAttTime());
        verify(mockBfdClient, never()).requestNextBundleFromServer(STU3, bundle1);
    }

    @Test
    void process_whenPatientHasNoEOBClaimsData() throws ExecutionException, InterruptedException {
        org.hl7.fhir.dstu3.model.Bundle bundle1 = new org.hl7.fhir.dstu3.model.Bundle();
        when(mockBfdClient.requestEOBFromServer(STU3, patientId, request.getAttTime())).thenReturn(bundle1);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(STU3, patientId, request.getAttTime());
        verify(mockBfdClient, never()).requestNextBundleFromServer(STU3, bundle1);
    }

    @Test
    void process_whenPatientHasSinglePageOfClaimsDataSince() throws ExecutionException, InterruptedException {
        // Override default behavior of setup
        coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(patientId), null, List.of(TestUtil.getOpenRange()));

        OffsetDateTime sinceDate = EARLY_ATT_DATE.plusDays(1);

        request = new PatientClaimsRequest(coverageSummary, LATER_ATT_DATE, sinceDate, "client", "job",
                "contractNum", noOpToken, STU3);

        org.hl7.fhir.dstu3.model.Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(STU3, patientId, LATER_ATT_DATE)).thenReturn(bundle1);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(STU3, patientId, LATER_ATT_DATE);
        verify(mockBfdClient, never()).requestNextBundleFromServer(STU3, bundle1);
    }

    @Test
    void process_whenPatientHasSinglePageOfClaimsDataEarlyAttDate() throws ExecutionException, InterruptedException {
        // Override default behavior of setup
        coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(patientId), null, List.of(TestUtil.getOpenRange()));

        request = new PatientClaimsRequest(coverageSummary, EARLY_ATT_DATE, null, "client", "job",
                "contractNum", noOpToken, STU3);

        org.hl7.fhir.dstu3.model.Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(STU3, patientId, null)).thenReturn(bundle1);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(STU3, patientId, null);
        verify(mockBfdClient, never()).requestNextBundleFromServer(STU3, bundle1);
    }

    @Test
    void process_whenPatientHasSinglePageOfClaimsDataEarlySinceDate() throws ExecutionException, InterruptedException {
        // Override default behavior of setup
        coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(patientId), null, List.of(TestUtil.getOpenRange()));

        request = new PatientClaimsRequest(coverageSummary, EARLY_ATT_DATE, EARLY_SINCE_DATE, "client", "job",
                "contractNum", noOpToken, STU3);

        org.hl7.fhir.dstu3.model.Bundle bundle1 = EobTestDataUtil.createBundle(eob.copy());
        when(mockBfdClient.requestEOBFromServer(STU3, patientId, null)).thenReturn(bundle1);

        cut.process(request).get();

        verify(mockBfdClient).requestEOBFromServer(STU3, patientId, null);
        verify(mockBfdClient, never()).requestNextBundleFromServer(STU3, bundle1);
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

    @Test
    void testFilterAsEob() {
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit b3 = new org.hl7.fhir.dstu3.model.ExplanationOfBenefit();
        org.hl7.fhir.r4.model.ExplanationOfBenefit b4 = new org.hl7.fhir.r4.model.ExplanationOfBenefit();
        assertFalse(gov.cms.ab2d.fhir.BundleUtils.isExplanationOfBenefitResource(null));
        assertTrue(gov.cms.ab2d.fhir.BundleUtils.isExplanationOfBenefitResource(b4));
        assertTrue(gov.cms.ab2d.fhir.BundleUtils.isExplanationOfBenefitResource(b3));
        org.hl7.fhir.r4.model.Patient patient = new org.hl7.fhir.r4.model.Patient();
        assertFalse(gov.cms.ab2d.fhir.BundleUtils.isExplanationOfBenefitResource(patient));
    }
}
