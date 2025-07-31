package gov.cms.ab2d.coveragecounts;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import gov.cms.ab2d.databasemanagement.DatabaseUtil;
import gov.cms.ab2d.snsclient.messages.CoverageCountDTO;
import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import gov.cms.ab2d.testutils.TestContext;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
class InvokeTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainerr = new AB2DPostgresqlContainer();

    private final ObjectMapper mapper = JsonMapper.builder()
            .configure(ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .build();

    @BeforeAll
    static void setup() throws SQLException, LiquibaseException {
        DatabaseUtil.setupDb(DatabaseUtil.getConnection());
    }

    @Test
    void
    coverageInvoke() throws JsonProcessingException {
        SNSEvent.SNSRecord message = new SNSEvent.SNSRecord();
        List<CoverageCountDTO> coverageCountDTO = Arrays.asList(
                new CoverageCountDTO("test", "test", 432432, 2024, 12, Timestamp.from(Instant.now())),
                new CoverageCountDTO("test2", "test2", 45434332, 2022, 9, Timestamp.from(Instant.now()))

        );
        System.out.println(mapper.writeValueAsString(coverageCountDTO));
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setMessage(mapper.writeValueAsString(coverageCountDTO));
        message.setSns(sns);
        SNSEvent event = new SNSEvent();
        event.setRecords(List.of(message));
        CoverageCountsHandler eventHandler = new CoverageCountsHandler();
        String value = mapper.writeValueAsString(event);
        assertDoesNotThrow(() -> {
            eventHandler.handleRequest(new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)), System.out, new TestContext());
        });
    }

    @Test
    void coverageInvokeFail() throws JsonProcessingException {
        SNSEvent.SNSRecord message = new SNSEvent.SNSRecord();
        List<CoverageCountDTO> coverageCountDTO = Arrays.asList(
                new CoverageCountDTO(null, null, 432432, 2024, 12, null),
                new CoverageCountDTO("test2", "test2", 45434332, 2022, 9, Timestamp.from(Instant.now()))

        );
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setMessage(mapper.writeValueAsString("dfds"));
        message.setSns(sns);
        SNSEvent event = new SNSEvent();
        event.setRecords(List.of(message));
        CoverageCountsHandler eventHandler = new CoverageCountsHandler();
        String value = mapper.writeValueAsString(event);
        ByteArrayInputStream array = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        TestContext context = new TestContext();
        assertThrows(CoverageCountException.class, () -> {
            eventHandler.handleRequest(array, System.out, context);
        });
    }

    @Test
    void testLog() throws NoSuchMethodException {
        CoverageCountsHandler eventHandler = new CoverageCountsHandler();
        TestContext context = new TestContext();

        Method m = CoverageCountsHandler.class.getDeclaredMethod("log", Exception.class, LambdaLogger.class);
        m.setAccessible(true);
        assertThrows(InvocationTargetException.class, () -> {
            m.invoke(eventHandler, new Exception("test"), context.getLogger());
        });
    }


}
