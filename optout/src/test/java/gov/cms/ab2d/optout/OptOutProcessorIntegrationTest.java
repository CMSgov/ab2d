package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.repository.OptOutRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
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
import static org.mockito.ArgumentMatchers.any;
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

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private static int mockServerPort = 8083;
    private static ClientAndServer mockServer;
    private static final String TEST_DIR = "test-data/";

    @BeforeAll
    public static void setupBFDClient() throws IOException {
        mockServer = ClientAndServer.startClientAndServer(mockServerPort);

        MockBfdServiceUtils.createMockServerMetaExpectation("test-data/meta.xml", mockServerPort);
        MockBfdServiceUtils.createMockServerPatientExpectation( TEST_DIR + "patientbundle.xml",
                mockServerPort, List.of());
    }

    @Before
    public void clearDB() {
        optOutRepo.deleteAll();
    }

    @AfterAll
    public static void tearDown() {
        mockServer.stop();
    }

    @Test
    @Transactional
    void process_shouldInsertRowsIntoOptOutTable()  {
        optOutRepo.deleteAll();

        final String testInputFile = "test-data/test-data.txt";
        final InputStream inputStream = getClass().getResourceAsStream("/" + testInputFile);
        final InputStreamReader isr = new InputStreamReader(inputStream);

        when(mockS3Gateway.listOptOutFiles()).thenReturn(List.of(testInputFile));
        when(mockS3Gateway.getOptOutFile(any())).thenReturn(isr);

        final List<OptOut> optOutRowsBeforeProcessing = optOutRepo.findAll();
        cut.process();
        final List<OptOut> optOutRowsAfterProcessing = optOutRepo.findAll();

        assertThat(optOutRowsBeforeProcessing, is(empty()));
        assertThat(optOutRowsAfterProcessing, is(not(empty())));

        assertThat(optOutRowsAfterProcessing.size(), is(7));

        final OptOut optOut = optOutRepo.findByCcwId("20010000001115").get(0);
        assertThat(optOut.getPolicyCode(), is("OPTOUT"));
        assertThat(optOut.getPurposeCode(), is("TREAT"));
        assertThat(optOut.getScopeCode(), is("patient-privacy"));
        assertThat(optOut.getLoIncCode(), is("64292-6"));
        assertThat(optOut.getEffectiveDate(), is(LocalDate.of(2019,10,24)));
        assertThat(optOut.getCcwId(), is("20010000001115"));

        verify(mockS3Gateway).listOptOutFiles();
        verify(mockS3Gateway).getOptOutFile(any());
    }
}