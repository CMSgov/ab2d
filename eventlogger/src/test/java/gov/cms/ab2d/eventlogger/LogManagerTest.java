package gov.cms.ab2d.eventlogger;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import gov.cms.ab2d.eventclient.clients.EventClient;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventlogger.eventloggers.kinesis.KinesisEventLogger;
import gov.cms.ab2d.eventlogger.eventloggers.slack.SlackLogger;
import gov.cms.ab2d.eventlogger.eventloggers.sql.SqlEventLogger;
import gov.cms.ab2d.eventclient.events.ErrorEvent;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
class LogManagerTest {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Container
    private static final AB2DLocalstackContainer localstackContainer  = new AB2DLocalstackContainer();

    private LogManager logManager;

    @Autowired
    AmazonSQSAsync amazonSQSAsync;

    @Mock
    private KinesisEventLogger kinesisEventLogger;

    @Mock
    private SlackLogger slackLogger;

    @Autowired
    private SqlEventLogger sqlEventLogger;

    @Autowired
    private LoggerEventRepository loggerEventRepository;

    @Mock
    private SQSEventClient sqsEventClient;


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void cleanUp() {
        reset(slackLogger);
        loggerEventRepository.delete();
    }

    @Test
    void log() {
        logManager = new LogManager(sqlEventLogger, kinesisEventLogger, slackLogger, sqsEventClient, false);
        ErrorEvent event = new ErrorEvent("user", "jobId", ErrorEvent.ErrorType.FILE_ALREADY_DELETED,
                "File Deleted");
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((ErrorEvent)args[0]).setAwsId("aws1111");
            return null; // void method, so return null
        }).when(kinesisEventLogger).log(event, true);

        logManager.log(event);
        assertEquals("aws1111", event.getAwsId());
        assertTrue(event.getId() > 0);

        List<LoggableEvent> events = loggerEventRepository.load(ErrorEvent.class);
        assertNotNull(events);
        assertEquals(1, events.size());
        ErrorEvent savedEvent = (ErrorEvent) events.get(0);
        assertEquals("aws1111", savedEvent.getAwsId());
    }

    @Test
    void logAndAlert() {
        logManager = new LogManager(sqlEventLogger, kinesisEventLogger, slackLogger, sqsEventClient, false);
        ErrorEvent event = new ErrorEvent("user", "jobId", ErrorEvent.ErrorType.FILE_ALREADY_DELETED,
                "File Deleted");
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((ErrorEvent)args[0]).setAwsId("aws1111");
            return null; // void method, so return null
        }).when(kinesisEventLogger).log(event, true);

        logManager.logAndAlert(event, Ab2dEnvironment.ALL);
        assertEquals("aws1111", event.getAwsId());
        assertTrue(event.getId() > 0);

        List<LoggableEvent> events = loggerEventRepository.load(ErrorEvent.class);
        assertNotNull(events);
        assertEquals(1, events.size());
        ErrorEvent savedEvent = (ErrorEvent) events.get(0);
        assertEquals("aws1111", savedEvent.getAwsId());

        verify(slackLogger, times(1)).logAlert(any(LoggableEvent.class), any());
    }

    @Test
    void logAndTrace() {
        logManager = new LogManager(sqlEventLogger, kinesisEventLogger, slackLogger, sqsEventClient, false);
        ErrorEvent event = new ErrorEvent("user", "jobId", ErrorEvent.ErrorType.FILE_ALREADY_DELETED,
                "File Deleted");
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((ErrorEvent)args[0]).setAwsId("aws1111");
            return null; // void method, so return null
        }).when(kinesisEventLogger).log(event, true);

        logManager.logAndTrace(event, Ab2dEnvironment.ALL);
        assertEquals("aws1111", event.getAwsId());
        assertTrue(event.getId() > 0);

        List<LoggableEvent> events = loggerEventRepository.load(ErrorEvent.class);
        assertNotNull(events);
        assertEquals(1, events.size());
        ErrorEvent savedEvent = (ErrorEvent) events.get(0);
        assertEquals("aws1111", savedEvent.getAwsId());

        verify(slackLogger, times(1)).logTrace(any(LoggableEvent.class), any());
    }

    @Test
    void testOnlySql() {
        logManager = new LogManager(sqlEventLogger, kinesisEventLogger, slackLogger, sqsEventClient, false);
        ErrorEvent event = new ErrorEvent("organization", "jobId", ErrorEvent.ErrorType.FILE_ALREADY_DELETED,
                "File Deleted");
        logManager.log(EventClient.LogType.SQL, event);
        assertNull(event.getAwsId());
        assertTrue(event.getId() > 0);

        List<LoggableEvent> events = loggerEventRepository.load(ErrorEvent.class);
        assertNotNull(events);
        assertEquals(1, events.size());
        ErrorEvent savedEvent = (ErrorEvent) events.get(0);
        assertNull(savedEvent.getAwsId());
    }

    @Test
    void testOnlyKin() {
        ErrorEvent event = new ErrorEvent("user", "jobId", ErrorEvent.ErrorType.FILE_ALREADY_DELETED,
                "File Deleted");
        logManager = new LogManager(sqlEventLogger, kinesisEventLogger, slackLogger, sqsEventClient, false);

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((ErrorEvent)args[0]).setAwsId("aws1111");
            return null; // void method, so return null
        }).when(kinesisEventLogger).log(event);

        logManager.log(EventClient.LogType.KINESIS, event);
        assertEquals("aws1111", event.getAwsId());
        List<LoggableEvent> events = loggerEventRepository.load(ErrorEvent.class);

        assertNotNull(events);
        assertNull(event.getId());
        assertEquals(0, events.size());
    }

    @Test
    void testAlert() {
        logManager = new LogManager(sqlEventLogger, kinesisEventLogger, slackLogger, sqsEventClient, false);
        ErrorEvent event = new ErrorEvent("user", "jobId", ErrorEvent.ErrorType.FILE_ALREADY_DELETED,
                "File Deleted");
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((ErrorEvent)args[0]).setAwsId("aws1111");
            return null; // void method, so return null
        }).when(kinesisEventLogger).log(event, true);

        logManager.alert(event.getDescription(), Ab2dEnvironment.ALL);

        verify(slackLogger, times(1)).logAlert(any(String.class), any());
    }

    @Test
    void testTrace() {
        logManager = new LogManager(sqlEventLogger, kinesisEventLogger, slackLogger, sqsEventClient, false);
        ErrorEvent event = new ErrorEvent("user", "jobId", ErrorEvent.ErrorType.FILE_ALREADY_DELETED,
                "File Deleted");
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((ErrorEvent)args[0]).setAwsId("aws1111");
            return null; // void method, so return null
        }).when(kinesisEventLogger).log(event, true);

        logManager.trace(event.getDescription(), Ab2dEnvironment.ALL);

        verify(slackLogger, times(1)).logTrace(any(String.class), any());
    }

    @Test
    void logWithSQS() {
        logManager = new LogManager(sqlEventLogger, kinesisEventLogger, slackLogger, sqsEventClient, true);
        ErrorEvent event = new ErrorEvent("user", "jobId", ErrorEvent.ErrorType.FILE_ALREADY_DELETED,
                "File Deleted");

        ArrayList<Ab2dEnvironment> enviroments = new ArrayList<>();
        enviroments.add(Ab2dEnvironment.LOCAL);
        logManager.log(event);
        logManager.trace(event.getDescription(), enviroments);
        logManager.alert(event.getDescription(), enviroments);
        logManager.log(EventClient.LogType.SQL, event);
        logManager.log(EventClient.LogType.KINESIS, event);
        logManager.logAndAlert(event, enviroments);
        logManager.logAndTrace(event, enviroments);

        verify(sqsEventClient, times(1)).sendLogs(any(LoggableEvent.class));
        verify(sqsEventClient, times(1)).trace(anyString(), any(ArrayList.class));
        verify(sqsEventClient, times(1)).alert(anyString(), any(ArrayList.class));
        verify(sqsEventClient, times(2)).log(any(EventClient.LogType.class), any(LoggableEvent.class));
        verify(sqsEventClient, times(1)).logAndAlert(any(LoggableEvent.class), any(ArrayList.class));
        verify(sqsEventClient, times(1)).logAndTrace(any(LoggableEvent.class), any(ArrayList.class));
    }
}