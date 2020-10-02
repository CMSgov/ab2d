package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.service.CoverageService;
import gov.cms.ab2d.common.service.InvalidJobStateTransition;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;

import static gov.cms.ab2d.worker.processor.CoverageMappingCallable.BENEFICIARY_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class CoverageProcessorImplTest {

    @Container
    private static final PostgreSQLContainer postgres = new AB2DPostgresqlContainer();

    @Value("${coverage.update.max.attempts}")
    private int maxRetries;

    @Autowired
    private SponsorRepository sponsorRepo;

    @Autowired
    private ContractRepository contractRepo;

    @Autowired
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private CoverageSearchRepository coverageSearchRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    private CoverageRepository coverageRepo;

    @Autowired
    private CoverageService coverageService;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    @Qualifier(value = "patientCoverageThreadPool")
    private ThreadPoolTaskExecutor taskExecutor;

    private Sponsor sponsor;
    private Contract contract;
    private CoveragePeriod period1Jan;

    private BFDClient bfdClient;

    private CoverageProcessorImpl processor;

    @BeforeEach
    void before() {

        sponsor = dataSetup.createSponsor("Cal Ripken", 200, "Cal Ripken Jr.", 201);
        contract = dataSetup.setupContract(sponsor, "TST-123");

        period1Jan = dataSetup.createCoveragePeriod(contract, 1, 2020);

        bfdClient = mock(BFDClient.class);

        processor = new CoverageProcessorImpl(coverageService, bfdClient, taskExecutor,
                3, 3, 3);
    }

    @AfterEach
    void after() {
        coverageRepo.deleteAll();
        coverageSearchEventRepo.deleteAll();
        coverageSearchRepo.deleteAll();
        coveragePeriodRepo.deleteAll();
        contractRepo.delete(contract);
        contractRepo.flush();

        if (sponsor != null) {
            sponsorRepo.delete(sponsor);
            sponsorRepo.flush();
        }
    }

    @DisplayName("Cannot submit twice")
    @Test
    void cannotSubmitTwice() {

        coverageService.submitSearch(period1Jan.getId(), "testing");

        assertEquals(1, coverageSearchRepo.count());

        processor.queueCoveragePeriod(period1Jan, false);

        assertEquals(1, coverageSearchRepo.count());
    }

    @DisplayName("Normal workflow functions")
    @Test
    void normalExecution() {

        Bundle bundle1 = buildBundle(0, 10);
        bundle1.setLink(Collections.singletonList(new Bundle.BundleLinkComponent().setRelation(Bundle.LINK_NEXT)));

        Bundle bundle2 = buildBundle(10, 20);

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(any(Bundle.class))).thenReturn(bundle2);

        processor.queueCoveragePeriod(period1Jan, false);
        JobStatus status = coverageService.getSearchStatus(period1Jan.getId());
        assertEquals(JobStatus.SUBMITTED, status);

        processor.mappingLoop();
        status = coverageService.getSearchStatus(period1Jan.getId());
        assertEquals(JobStatus.IN_PROGRESS, status);

        processor.monitorMapping();
        status = coverageService.getSearchStatus(period1Jan.getId());
        assertEquals(JobStatus.IN_PROGRESS, status);

        processor.insertionLoop();
        status = coverageService.getSearchStatus(period1Jan.getId());
        assertEquals(JobStatus.SUCCESSFUL, status);
    }

    @DisplayName("Mapping failure leads to retry but still can succeed on retry")
    @Test
    void mappingRetried() {

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenThrow(new RuntimeException("oops"));

        processor.queueCoveragePeriod(period1Jan, false);
        JobStatus status = coverageService.getSearchStatus(period1Jan.getId());
        assertEquals(JobStatus.SUBMITTED, status);

        processor.mappingLoop();
        status = coverageService.getSearchStatus(period1Jan.getId());
        assertEquals(JobStatus.IN_PROGRESS, status);

        processor.monitorMapping();
        assertTrue(coverageSearchEventRepo.findAll().stream().anyMatch(event -> event.getNewStatus() == JobStatus.FAILED));

        status = coverageService.getSearchStatus(period1Jan.getId());
        assertEquals(JobStatus.SUBMITTED, status);

        reset(bfdClient);

        Bundle bundle1 = buildBundle(0, 10);
        bundle1.setLink(Collections.singletonList(new Bundle.BundleLinkComponent().setRelation(Bundle.LINK_NEXT)));

        Bundle bundle2 = buildBundle(10, 20);

        Mockito.clearInvocations();
        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(any(Bundle.class))).thenReturn(bundle2);

        processor.mappingLoop();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {}

        processor.monitorMapping();


        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {}

        processor.insertionLoop();

        status = coverageService.getSearchStatus(period1Jan.getId());
        assertEquals(JobStatus.SUCCESSFUL, status);
    }

    @DisplayName("Mapping failure after x retries")
    @Test
    void mappingFailsAfterXRetries() {

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenThrow(new RuntimeException("oops"));


        processor.queueCoveragePeriod(period1Jan, false);
        JobStatus status = coverageService.getSearchStatus(period1Jan.getId());
        assertEquals(JobStatus.SUBMITTED, status);

        // Should retry x times
        for (int i = 0; i < maxRetries; i++) {
            status = iterateFailingJob();
            assertEquals(JobStatus.SUBMITTED, status);
        }

        status = iterateFailingJob();
        assertEquals(JobStatus.FAILED, status);
    }

    private JobStatus iterateFailingJob() {
        JobStatus status;
        processor.mappingLoop();
        status = coverageService.getSearchStatus(period1Jan.getId());
        assertEquals(JobStatus.IN_PROGRESS, status);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {}

        processor.monitorMapping();
        status = coverageService.getSearchStatus(period1Jan.getId());
        return status;
    }

    private Bundle buildBundle(int startIndex, int endIndex) {
        Bundle bundle1 = new Bundle();

        for (int i = startIndex; i < endIndex; i++) {
            Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
            Patient patient = new Patient();

            Identifier identifier = new Identifier();
            identifier.setSystem(BENEFICIARY_ID);
            identifier.setValue("test-" + i);

            patient.setIdentifier(Collections.singletonList(identifier));
            component.setResource(patient);

            bundle1.addEntry(component);
        }
        return bundle1;
    }
}
