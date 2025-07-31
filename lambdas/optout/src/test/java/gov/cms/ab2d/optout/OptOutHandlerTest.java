package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.event.S3EventNotification;
import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import static gov.cms.ab2d.optout.OptOutConstantsTest.*;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Testcontainers
class OptOutHandlerTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();
    private final static OptOutHandler handler = spy(new OptOutHandler());
    private final static SQSEvent sqsEvent = mock(SQSEvent.class);
    private final static SQSEvent.SQSMessage sqsMessage = mock(SQSEvent.SQSMessage.class);

    @BeforeAll
    static void beforeAll() throws IOException {
        when(sqsEvent.getRecords()).thenReturn(Collections.singletonList(sqsMessage));
        when(sqsMessage.getBody()).thenReturn(getPayload());
    }

    @Test
    void getBucketAndFileNamesTest() throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(getPayload());
        var s3EventMessage = json.get("Message");

        var notification = S3EventNotification.parseJson(s3EventMessage.toString()).getRecords().get(0);

        assertEquals(TEST_BUCKET_PATH + "/in/" + TEST_FILE_NAME, handler.getFileName(notification));
        assertEquals(TEST_BFD_BUCKET_NAME, handler.getBucketName(notification));
    }

    @Test
    void optOutHandlerInvoke() {
        Context context = mock(Context.class);
        LambdaLogger logger = mock(LambdaLogger.class);
        when(context.getLogger()).thenReturn(logger);

        assertThrows(OptOutException.class, () -> handler.handleRequest(sqsEvent, context));
        verify(handler, times(1)).processSQSMessage(sqsMessage, context);
        verify(logger, times(2)).log(anyString());
    }

    @Test
    void itLogsResults() {
        LambdaLogger logger = mock(LambdaLogger.class);
        OptOutResults optOutResults = new OptOutResults(1, 1, 2, 2);

        handler.logResults(optOutResults, logger);
        verify(logger, times(1)).log(anyString());
    }

    @Test
    void itDoesNotLogWhenResultsAreNull() {
        Context context = mock(Context.class);
        LambdaLogger logger = mock(LambdaLogger.class);

        when(context.getLogger()).thenReturn(logger);
        handler.logResults(null, logger);
        verify(logger, times(0)).log(anyString());
    }

    static private String getPayload() throws IOException {
        return Files.readString(Paths.get("src/test/resources/sqsEvent.json"));
    }
}
