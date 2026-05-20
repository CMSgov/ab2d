package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
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

@Testcontainers(disabledWithoutDocker = true)
class OptOutHandlerTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();
    private static final OptOutHandler HANDLER = spy(new OptOutHandler());
    private static final SQSEvent SQS_EVENT = mock(SQSEvent.class);
    private static final SQSEvent.SQSMessage SQS_MESSAGE = mock(SQSEvent.SQSMessage.class);

    @BeforeAll
    static void beforeAll() throws IOException {
        when(SQS_EVENT.getRecords()).thenReturn(Collections.singletonList(SQS_MESSAGE));
        when(SQS_MESSAGE.getBody()).thenReturn(getPayload());
    }

    @Test
    void getBucketAndFileNamesTest() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(getPayload());
        var s3EventMessage = json.get("Message").asText();

        var notification = S3EventNotification.parseJson(s3EventMessage).getRecords().get(0);

        assertEquals(TEST_BUCKET_PATH + "/in/" + TEST_FILE_NAME, HANDLER.getFileName(notification));
        assertEquals(TEST_BFD_BUCKET_NAME, HANDLER.getBucketName(notification));
    }

    @Test
    void optOutHandlerInvoke() {
        Context context = mock(Context.class);
        LambdaLogger logger = mock(LambdaLogger.class);
        when(context.getLogger()).thenReturn(logger);

        assertThrows(OptOutException.class, () -> HANDLER.handleRequest(SQS_EVENT, context));
        verify(HANDLER, times(1)).processSQSMessage(SQS_MESSAGE, context);
        verify(logger, times(2)).log(anyString());
    }

    @Test
    void itLogsResults() {
        LambdaLogger logger = mock(LambdaLogger.class);
        OptOutResults optOutResults = new OptOutResults(1, 1, 2, 2);

        HANDLER.logResults(optOutResults, logger);
        verify(logger, times(1)).log(anyString());
    }

    @Test
    void itDoesNotLogWhenResultsAreNull() {
        Context context = mock(Context.class);
        LambdaLogger logger = mock(LambdaLogger.class);

        when(context.getLogger()).thenReturn(logger);
        HANDLER.logResults(null, logger);
        verify(logger, times(0)).log(anyString());
    }

    private static String getPayload() throws IOException {
        return Files.readString(Paths.get("src/test/resources/sqsEvent.json"));
    }
}
