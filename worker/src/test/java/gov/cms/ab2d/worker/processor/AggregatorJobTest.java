package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.Identifiers;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.config.SearchConfig;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Future;

import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
class AggregatorJobTest {
    @Container
    private static final PostgreSQLContainer postgres = new AB2DPostgresqlContainer();

    PatientClaimsProcessor processor;

    @TempDir
    File tempDir;

    @Mock
    private BFDClient bfdClient;

    @Mock
    private LogManager logManager;

    private static final String STREAMING = "streaming";
    private static final String FINISHED = "finished";

    @BeforeEach
    void setUp() {
        SearchConfig searchConfig = new SearchConfig(tempDir.getAbsolutePath(), STREAMING,
                FINISHED, 0, 0, 1, 2);

        processor = new PatientClaimsProcessorImpl(bfdClient, logManager, searchConfig);
    }

    @Test
    void testWriteBunch() throws ParseException, IOException, InterruptedException {
        String job = "123";
        String contractNo = "ABCD";
        String org = "org1";
        final Token token = NewRelic.getAgent().getTransaction().getToken();

        when(bfdClient.requestEOBFromServer(eq(STU3), eq(1L), any())).thenReturn(BundleUtils.createBundle(createBundleEntry(1)));
        when(bfdClient.requestEOBFromServer(eq(STU3), eq(2L), any())).thenReturn(BundleUtils.createBundle(createBundleEntry(2)));
        when(bfdClient.requestEOBFromServer(eq(STU3), eq(3L), any())).thenReturn(BundleUtils.createBundle(createBundleEntry(3)));
        when(bfdClient.requestEOBFromServer(eq(STU3), eq(4L), any())).thenReturn(BundleUtils.createBundle(createBundleEntry(4)));
        when(bfdClient.requestEOBFromServer(eq(STU3), eq(5L), any())).thenReturn(BundleUtils.createBundle(createBundleEntry(5)));
        when(bfdClient.requestEOBFromServer(eq(STU3), eq(6L), any())).thenReturn(BundleUtils.createBundle(createBundleEntry(6)));
        when(bfdClient.requestEOBFromServer(eq(STU3), eq(7L), any())).thenReturn(BundleUtils.createBundle(createBundleEntry(7)));
        when(bfdClient.requestEOBFromServer(eq(STU3), eq(8L), any())).thenReturn(BundleUtils.createBundle(createBundleEntry(8)));
        when(bfdClient.requestEOBFromServer(eq(STU3), eq(9L), any())).thenReturn(BundleUtils.createBundle(createBundleEntry(9)));
        when(bfdClient.requestEOBFromServer(eq(STU3), eq(10L), any())).thenReturn(BundleUtils.createBundle(createBundleEntry(10)));

        ContractForCoverageDTO contract = createContract(contractNo);

        PatientClaimsRequest request = new PatientClaimsRequest(createCoverageSummaries(10, contract),
                OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 9, ZoneOffset.UTC),
                OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 9, ZoneOffset.UTC),
                org, job, contract.getContractNumber(), Contract.ContractType.NORMAL, token, FhirVersion.STU3, tempDir.getAbsolutePath());
        ReflectionTestUtils.setField(processor, "earliestDataDate", "01/01/2020");

        Future<ProgressTrackerUpdate> future = processor.process(request);
        while (!future.isDone()) {
            Thread.sleep(500);
        }

        File[] files = (new File(tempDir + "/" + job + "/" + FINISHED)).listFiles();
        assertNotNull(files);
        assertEquals(1, files.length);
        List<String> allLines = Files.readAllLines(Path.of(files[0].getAbsolutePath()));
        assertEquals(10, allLines.size());
        assertTrue(allLines.get(0).contains("Patient/1"));
        assertTrue(allLines.get(1).contains("Patient/2"));
        assertTrue(allLines.get(9).contains("Patient/10"));
    }

    Identifiers createIdentifier(int id) {
        return new Identifiers(id, "M" + id, new LinkedHashSet<>());
    }

    CoverageSummary createCoverageSummary(int id, ContractForCoverageDTO contract) {
        FilterOutByDate.DateRange range = FilterOutByDate.getDateRange(1, 2020,
                12, 2099);
        return new CoverageSummary(createIdentifier(id), contract, List.of(range));
    }

    List<CoverageSummary> createCoverageSummaries(int number, ContractForCoverageDTO contract) {
        List<CoverageSummary> summaries = new ArrayList<>();
        for (int i = 1; i <= number; i++) {
            summaries.add(createCoverageSummary(i, contract));
        }
        return summaries;
    }

    ContractForCoverageDTO createContract(String contractNumber) {
        ContractForCoverageDTO contract = new ContractForCoverageDTO();
        contract.setContractType(ContractForCoverageDTO.ContractType.NORMAL);
        contract.setContractNumber(contractNumber);
        contract.setAttestedOn(OffsetDateTime.MIN);
        return contract;
    }

    ExplanationOfBenefit createEob(int patientId) throws ParseException {
        final SimpleDateFormat SDF = new SimpleDateFormat("MM/dd/yyyy");

        ExplanationOfBenefit eob = (ExplanationOfBenefit) EobTestDataUtil.createEOB();
        eob.getBillablePeriod().setStart(SDF.parse("02/13/1970"));
        eob.getBillablePeriod().setEnd(new Date());
        eob.getMeta().setLastUpdated(new Date());
        eob.setPatient(new org.hl7.fhir.dstu3.model.Reference().setReference("Patient/" + patientId));
        return eob;
    }

    public org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent createBundleEntry(int patientId) throws ParseException {
        var component = new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent();
        component.setResource(createEob(patientId));
        return component;
    }
}
