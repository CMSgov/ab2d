package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.repository.OptOutRepository;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
class OptOutClientServiceTest {
    @Autowired
    private OptOutConverterService cut;
    @Autowired
    private OptOutRepository optOutRepository;

    private static int mockServerPort = 8083;
    private static ClientAndServer mockServer;
    private static final String TEST_DIR = "test-data/";

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeAll
    public static void setupBFDClient() throws IOException {
        mockServer = ClientAndServer.startClientAndServer(mockServerPort);

        MockBfdServiceUtils.createMockServerMetaExpectation("test-data/meta.xml", mockServerPort);
        MockBfdServiceUtils.createMockServerPatientExpectation( TEST_DIR + "patientbundle.xml",
                mockServerPort, List.of());
    }

    @AfterAll
    public static void tearDown() {
        mockServer.stop();
    }

    @Test
    public void getOptOut() {
        final String line = getLinesFromFile().skip(6).limit(1).collect(Collectors.toList()).get(0);
        final List<OptOut> optOut = cut.convert(line);
        assertNotNull(optOut);
        assertEquals(1, optOut.size());
        assertEquals("19990000000157", optOut.get(0).getCcwId());
        optOut.forEach(o -> optOutRepository.save(o));
    }

    private Stream<String> getLinesFromFile() {
        final String testInputFile = "test-data/test-data.txt";
        final InputStream inputStream = getClass().getResourceAsStream("/" + testInputFile);
        final InputStreamReader isr = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(isr);
        return bufferedReader.lines();
    }
}