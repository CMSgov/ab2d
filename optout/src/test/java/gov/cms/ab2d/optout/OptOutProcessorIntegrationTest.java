package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.repository.OptOutRepository;
import gov.cms.ab2d.common.util.MockBfdServiceUtils;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.*;
import gov.cms.ab2d.eventlogger.reports.sql.DoAll;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import gov.cms.ab2d.optout.gateway.S3Gateway;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
class OptOutProcessorIntegrationTest {

    @MockBean
    private Scheduler scheduler;

    @MockBean
    private S3Gateway mockS3Gateway;

    @Autowired
    private OptOutRepository optOutRepo;

    @Autowired
    private OptOutProcessor cut;

    @Autowired
    private DoAll doAll;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private static int mockServerPort = 8083;
    private static ClientAndServer mockServer;
    private static final String TEST_DIR = "test-data/";

    @BeforeAll
    public static void setupBFDClient() throws IOException {
        mockServer = ClientAndServer.startClientAndServer(mockServerPort);

        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta.xml", mockServerPort);
        MockBfdServiceUtils.createMockServerPatientExpectation( TEST_DIR + "patientbundle.xml",
                mockServerPort, List.of());
    }

    @Before
    public void clearDB() {
        optOutRepo.deleteAll();
        doAll.delete();
    }

    @AfterAll
    public static void tearDown() {
        mockServer.stop();
    }

    @Test
    @Transactional
    void process_shouldInsertRowsIntoOptOutTable()  {
        optOutRepo.deleteAll();

        final String testInputFile = "test-data.txt";
        final InputStream inputStream = getClass().getResourceAsStream("/" + testInputFile);
        final InputStreamReader isr = new InputStreamReader(inputStream);

        final String testInputFileSecondary = "test-data-secondary-file.txt";
        final InputStream inputStreamSecondary = getClass().getResourceAsStream("/" + testInputFileSecondary);
        final InputStreamReader isrSecondary = new InputStreamReader(inputStreamSecondary);

        when(mockS3Gateway.listOptOutFiles()).thenReturn(List.of(testInputFile, testInputFileSecondary));
        when(mockS3Gateway.getOptOutFile(testInputFile)).thenReturn(isr);
        when(mockS3Gateway.getOptOutFile(testInputFileSecondary)).thenReturn(isrSecondary);

        final List<OptOut> optOutRowsBeforeProcessing = optOutRepo.findAll();
        cut.process();
        final List<OptOut> optOutRowsAfterProcessing = optOutRepo.findAll();

        assertThat(optOutRowsBeforeProcessing, is(empty()));
        assertThat(optOutRowsAfterProcessing, is(not(empty())));

        assertThat(optOutRowsAfterProcessing.size(), is(10));

        final OptOut optOut = optOutRepo.findByCcwId("20010000001115").get(0);
        assertThat(optOut.getPolicyCode(), is("OPTOUT"));
        assertThat(optOut.getPurposeCode(), is("TREAT"));
        assertThat(optOut.getScopeCode(), is("patient-privacy"));
        assertThat(optOut.getLoIncCode(), is("64292-6"));
        assertThat(optOut.getEffectiveDate(), is(LocalDate.of(2019,10,24)));
        assertThat(optOut.getCcwId(), is("20010000001115"));
        assertThat(optOut.getFilename(), is("test-data.txt"));

        verify(mockS3Gateway).listOptOutFiles();
        verify(mockS3Gateway, times(2)).getOptOutFile(any());

        List<LoggableEvent> reloadEvents = doAll.load(ReloadEvent.class);
        assertEquals(2, reloadEvents.size());
        ReloadEvent requestEvent = (ReloadEvent) reloadEvents.get(0);
        assertEquals(ReloadEvent.FileType.OPT_OUT, requestEvent.getFileType());
        assertEquals(testInputFile, requestEvent.getFileName());
        assertEquals(24, requestEvent.getNumberLoaded());

        assertTrue(UtilMethods.allEmpty(
                doAll.load(ApiRequestEvent.class),
                doAll.load(ApiResponseEvent.class),
                doAll.load(ContractBeneSearchEvent.class),
                doAll.load(ErrorEvent.class),
                doAll.load(FileEvent.class),
                doAll.load(JobStatusChangeEvent.class)
        ));

        // Verify files don't get processed again
        cut.process();

    }

    @Test
    public void testShouldProcessDifferentFiles() {
        OptOut optOut = new OptOut();

    }
}