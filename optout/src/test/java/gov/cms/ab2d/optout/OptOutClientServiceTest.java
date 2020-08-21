package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.repository.OptOutRepository;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.MockBfdServiceUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

@Disabled
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

        MockBfdServiceUtils.createMockServerMetaExpectation(TEST_DIR + "meta.xml", mockServerPort);
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
        assertEquals(2, optOut.size());
        assertEquals("20010000001115", optOut.get(0).getCcwId());
        optOut.forEach(o -> {
            o.setFilename("test_filename");
            optOutRepository.save(o);
        });
    }

    private Stream<String> getLinesFromFile() {
        final String testInputFile = "test-data.txt";
        final InputStream inputStream = getClass().getResourceAsStream("/" + testInputFile);
        final InputStreamReader isr = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(isr);
        return bufferedReader.lines();
    }
}