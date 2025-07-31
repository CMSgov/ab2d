package gov.cms.ab2d.attributiondatashare;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.mockrunner.mock.jdbc.MockResultSet;
import gov.cms.ab2d.lambdalibs.lib.FileUtil;
import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;

import static gov.cms.ab2d.attributiondatashare.AttributionDataShareConstants.*;
import static gov.cms.ab2d.attributiondatashare.AttributionDataShareHelper.getExecuteQuery;
import static gov.cms.ab2d.attributiondatashare.S3MockAPIExtension.getBucketName;
import static gov.cms.ab2d.attributiondatashare.S3MockAPIExtension.getUploadPath;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@ExtendWith({S3MockAPIExtension.class})
class AttributionDataShareTest {
    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();
    private static final LambdaLogger LOGGER = mock(LambdaLogger.class);
    private static final String FILE_NAME = REQ_FILE_NAME + new SimpleDateFormat(REQ_FILE_NAME_PATTERN).format(new Date());
    private static final String FILE_FULL_PATH = FILE_PATH + FILE_NAME;
    private static final String MBI1 = "DUMMY000001";
    private static final String MBI2 = "DUMMY000002";
    private static final Date DATE = new GregorianCalendar(2024, Calendar.FEBRUARY, 26).getTime();
    private AttributionDataShareHelper helper;

    @BeforeEach
    public void beforeEach() {
        helper = spy(new AttributionDataShareHelper(FILE_NAME, FILE_FULL_PATH, LOGGER));
    }

    @Test
    void copyDataToFileTest() throws IOException, SQLException {
        var connection = mock(Connection.class);
        var stmt = mock(Statement.class);
        var rs = new MockResultSet("");
        rs.addColumn("mbi", Arrays.asList(MBI1, MBI2));
        rs.addColumn("effective_date", Arrays.asList("2024-02-26", null));
        rs.addColumn("share_data", Arrays.asList(true, null));
        when(connection.createStatement()).thenReturn(stmt);

        when(getExecuteQuery(stmt)).thenReturn(rs);
        assertDoesNotThrow(() -> helper.copyDataToFile(connection));

        assertTrue(Files.exists(Paths.get(FILE_FULL_PATH)));

        var scanner = new Scanner(Paths.get(FILE_FULL_PATH), StandardCharsets.UTF_8);
        var content = scanner.useDelimiter("\\A").next();
        scanner.close();

        assertTrue(content.contains(AB2D_HEADER_REQ));
        assertTrue(content.contains(AB2D_TRAILER_REQ));

        FileUtil.deleteDirectoryRecursion(Paths.get(FILE_FULL_PATH));
    }

    @Test
    void getResponseLineTest() {
        var line1 = helper.getResponseLine(MBI1, null, null);
        var line2 = helper.getResponseLine(MBI2, DATE, false);
        var line3 = helper.getResponseLine("A", DATE, true);
        assertEquals(20, line1.length());
        assertEquals(20, line2.length());
        assertEquals(20, line3.length());
        assertEquals(MBI1 + "         ", line1);
        assertEquals(MBI2 + "20240226N", line2);
        assertEquals("A          20240226Y", line3);
    }


    @Test
    void writeFileToFinalDestinationTest() throws IOException {
        createTestFile();
        helper.uploadToS3(S3MockAPIExtension.S3_CLIENT);
        assertTrue(S3MockAPIExtension.isObjectExists(FILE_NAME));
        S3MockAPIExtension.deleteFile(FILE_NAME);
    }

    @Test
    void getBucketNameTest() {
        assertEquals(getBucketName(), helper.getBucketName());
    }

    @Test
    void getUploadPathTest() {
        assertEquals(getUploadPath(), helper.getUploadPath());
    }

    private void createTestFile() throws IOException {
        PrintWriter writer = new PrintWriter(FILE_FULL_PATH, StandardCharsets.UTF_8);
        writer.println(MBI1);
        writer.close();
    }
}
