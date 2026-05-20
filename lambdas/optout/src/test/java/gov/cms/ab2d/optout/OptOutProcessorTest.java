package gov.cms.ab2d.optout;


import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.cms.ab2d.lambdalibs.lib.ParameterStoreUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gov.cms.ab2d.optout.OptOutConstants.*;
import static gov.cms.ab2d.optout.OptOutConstantsTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({S3MockAPIExtension.class, SystemStubsExtension.class})
class OptOutProcessorTest {
    private static final ResultSet RESULT_SET = mock(ResultSet.class);
    private static final PreparedStatement PREPARED_STATEMENT = mock(PreparedStatement.class);
    private static final Connection DB_CONNECTION = mock(Connection.class);
    private static final MockedStatic<ParameterStoreUtil> PARAMETER_STORE = mockStatic(ParameterStoreUtil.class);
    private static MockedStatic<DriverManager> driverManager;
    private static final String DATE = new SimpleDateFormat(EFFECTIVE_DATE_PATTERN).format(new Date());
    private static final String MBI1 = "DUMMY000001";
    private static final String MBI2 = "DUMMY000002";
    private static final String TRAILER_COUNT = "0000000001";

    private static String validLine(char isOptOut) {
        return MBI1 + isOptOut;
    }

    private static OptOutProcessor optOutProcessing;

    @BeforeAll
    static void beforeAll() throws SQLException {
        driverManager = mockStatic(DriverManager.class);
        driverManager.when(() -> DriverManager.getConnection(any(), any(), any()))
                .thenReturn(DB_CONNECTION);

        when(DB_CONNECTION.prepareStatement(anyString())).thenReturn(PREPARED_STATEMENT);
        when(DB_CONNECTION.createStatement()).thenReturn(PREPARED_STATEMENT);
        when(PREPARED_STATEMENT.executeQuery(anyString())).thenReturn(RESULT_SET);
    }

    @BeforeEach
    void beforeEach(EnvironmentVariables env) throws IOException {
        env.set("ENV", "test");
        S3MockAPIExtension.createFile(Files.readString(Paths.get("src/test/resources/" + TEST_FILE_NAME), StandardCharsets.UTF_8), TEST_FILE_NAME);
        PARAMETER_STORE.
                when(() -> ParameterStoreUtil.getParameterStore(any(), any(), any()))
                .thenReturn(new ParameterStoreUtil("", "", ""));
        optOutProcessing = spy(new OptOutProcessor(mock(LambdaLogger.class)));
        optOutProcessing.setRejected(false);
    }

    @AfterEach
    void afterEach() {
        S3MockAPIExtension.deleteFile(TEST_FILE_NAME);
    }

    @AfterAll
    static void afterAll() {
        driverManager.close();
    }

    @Test
    void processTest() throws URISyntaxException {
        optOutProcessing.setRejected(false);
        OptOutResults results = optOutProcessing.process(TEST_FILE_NAME, TEST_BFD_BUCKET_NAME, TEST_ENDPOINT);
        assertEquals(7, optOutProcessing.getOptOutInformationList().size());

        assertEquals(3, results.getOptInToday());
        assertEquals(4, results.getOptOutToday());
        assertEquals(optOutProcessing.getOptOutInformationList().size(), results.getTotalToday());
    }

    @Test
    void multipleMBIsProcessTest() throws URISyntaxException, IOException {
        S3MockAPIExtension.createFile(Files.readString(Paths.get("src/test/resources/" + MULTIPLE_MBIS_TEST_FILE_NAME), StandardCharsets.UTF_8), MULTIPLE_MBIS_TEST_FILE_NAME);
        optOutProcessing.setRejected(false);
        OptOutResults results = optOutProcessing.process(MULTIPLE_MBIS_TEST_FILE_NAME, TEST_BFD_BUCKET_NAME, TEST_ENDPOINT);
        assertEquals(9, optOutProcessing.getOptOutInformationList().size());

        assertEquals(4, results.getOptInToday());
        assertEquals(5, results.getOptOutToday());
        assertEquals(optOutProcessing.getOptOutInformationList().size(), results.getTotalToday());
    }

    @Test
    void processEmptyFileTest() throws IOException, URISyntaxException {
        var emptyFileName = "emptyDummy.txt";
        S3MockAPIExtension.createFile(Files.readString(Paths.get("src/test/resources/" + emptyFileName), StandardCharsets.UTF_8), emptyFileName);
        OptOutResults results = optOutProcessing.process(emptyFileName, TEST_BFD_BUCKET_NAME, TEST_ENDPOINT);
        assertEquals(0, optOutProcessing.getOptOutInformationList().size());
        assertEquals(optOutProcessing.getOptOutInformationList().size(), results.getTotalToday());
        S3MockAPIExtension.deleteFile(emptyFileName);
    }

    @Test
    void createTrueOptOutInformationTest() {
        optOutProcessing.createOptOutInformation(validLine('Y'));
        var list = optOutProcessing.getOptOutInformationList();
        assertEquals(1, list.size());
        assertEquals(MBI1, list.get(0).getMbi());
        assertTrue(list.get(0).getOptOutFlag());
    }

    @Test
    void createFalseOptOutInformationTest() {
        optOutProcessing.createOptOutInformation(validLine('N'));
        var list = optOutProcessing.getOptOutInformationList();
        assertEquals(1, list.size());
        assertEquals(MBI1, list.get(0).getMbi());
        assertFalse(list.get(0).getOptOutFlag());
    }

    @Test
    void createMultipleOptOutInformationTest() {
        optOutProcessing.createOptOutInformation(MBI1 + "," + MBI2 + "Y");
        var list = optOutProcessing.getOptOutInformationList();
        assertEquals(2, list.size());
        assertEquals(MBI1, list.get(0).getMbi());  // MBI1
        assertTrue(list.get(0).getOptOutFlag());   // Y
        assertEquals(MBI2, list.get(1).getMbi());  // MBI2
        assertTrue(list.get(1).getOptOutFlag());   // Y
    }

    @Test
    void createAcceptedResponseTest() {
        optOutProcessing.getOptOutInformationList().add(new OptOutInformation(MBI1, true));
        var expectedLine = MBI1 + DATE + "Y" + RecordStatus.ACCEPTED;
        var expectedText = AB2D_HEADER_CONF + DATE + LINE_SEPARATOR
                + expectedLine + LINE_SEPARATOR
                + AB2D_TRAILER_CONF + DATE + TRAILER_COUNT;
        assertEquals(expectedText, optOutProcessing.createResponseContent());
    }

    @Test
    void createRejectedResponseTest() {
        optOutProcessing.setRejected(true);
        optOutProcessing.getOptOutInformationList().add(new OptOutInformation(MBI1, false));
        var expectedLine = MBI1 + "        " + "N" + RecordStatus.REJECTED;
        var expectedText = AB2D_HEADER_CONF + DATE + LINE_SEPARATOR
                + expectedLine + LINE_SEPARATOR
                + AB2D_TRAILER_CONF + DATE + TRAILER_COUNT;
        assertEquals(expectedText, optOutProcessing.createResponseContent());
    }

    @Test
    void updateOptOutTest() {
        optOutProcessing.updateOptOut();
        assertFalse(optOutProcessing.isRejected());
    }

    @Test
    void updateOptOutExceptionTest() throws SQLException {
        when(DB_CONNECTION.prepareStatement(anyString())).thenThrow(SQLException.class);
        optOutProcessing.updateOptOut();
        // Insertion error exists
        assertTrue(optOutProcessing.isRejected());
        assertTrue(S3MockAPIExtension.isObjectExists(TEST_FILE_NAME));
    }

    @Test
    void getEffectiveDateTest() {
        optOutProcessing.setRejected(false);
        assertEquals(DATE, optOutProcessing.getEffectiveDate(DATE));
        optOutProcessing.setRejected(true);
        assertEquals("        ", optOutProcessing.getEffectiveDate(DATE));
    }

    @Test
    void getRecordStatusTest() {
        optOutProcessing.setRejected(false);
        assertEquals(RecordStatus.ACCEPTED.toString(), optOutProcessing.getRecordStatus());
        optOutProcessing.setRejected(true);
        assertEquals(RecordStatus.REJECTED.toString(), optOutProcessing.getRecordStatus());
    }

    @Test
    void getOptOutResultsTest() throws SQLException {
        final String optInResultSetString = "optin";
        final String optOutResultSetString = "optout";

        final int optInTotalCount = 9;
        final int optOutTotalCount = 7;

        when(RESULT_SET.next()).thenReturn(true).thenReturn(false);
        when(RESULT_SET.getInt(optInResultSetString)).thenReturn(optInTotalCount);
        when(RESULT_SET.getInt(optOutResultSetString)).thenReturn(optOutTotalCount);

        optOutProcessing.getOptOutInformationList().add(new OptOutInformation(MBI1, true));
        optOutProcessing.getOptOutInformationList().add(new OptOutInformation("DUMMY000002", false));

        OptOutResults results = optOutProcessing.getOptOutResults();
        assertNotNull(results);
        assertEquals(1, results.getOptInToday());
        assertEquals(1, results.getOptOutToday());
        assertEquals(optInTotalCount, results.getOptInTotal());
        assertEquals(optOutTotalCount, results.getOptOutTotal());
    }

    @Test
    void testDummyMbiNonProd() {
        assertDoesNotThrow(() -> optOutProcessing.process(TEST_FILE_NAME, TEST_BFD_BUCKET_NAME, TEST_ENDPOINT));
    }

}
