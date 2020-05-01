package gov.cms.ab2d.eventlogger;

import gov.cms.ab2d.eventlogger.eventloggers.kinesis.KinesisEventLogger;
import gov.cms.ab2d.eventlogger.eventloggers.sql.SqlEventLogger;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.eventlogger.reports.sql.DoAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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

    private LogManager logManager;

    @Mock
    private KinesisEventLogger kinesisEventLogger;

    @Autowired
    private SqlEventLogger sqlEventLogger;

    @Autowired
    private DoAll doAll;

    @Test
    void log() {
        logManager = new LogManager(sqlEventLogger, kinesisEventLogger);
        ErrorEvent event = new ErrorEvent("user", "jobId", ErrorEvent.ErrorType.FILE_ALREADY_DELETED,
                "File Deleted");
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                ((ErrorEvent)args[0]).setAwsId("aws1111");
                return null; // void method, so return null
            }
        }).when(kinesisEventLogger).log(event);

        logManager.log(event);
        assertEquals("aws1111", event.getAwsId());
        assertTrue(event.getId() > 0);

        List<LoggableEvent> events = doAll.load(ErrorEvent.class);
        assertNotNull(events);
        assertEquals(1, events.size());
        ErrorEvent savedEvent = (ErrorEvent) events.get(0);
        assertEquals("aws1111", savedEvent.getAwsId());

        doAll.delete();
    }

    @Test
    void testOnlySql() {
        logManager = new LogManager(sqlEventLogger, kinesisEventLogger);
        ErrorEvent event = new ErrorEvent("user", "jobId", ErrorEvent.ErrorType.FILE_ALREADY_DELETED,
                "File Deleted");
        logManager.log(LogManager.LogType.SQL, event);
        assertNull(event.getAwsId());
        assertTrue(event.getId() > 0);

        List<LoggableEvent> events = doAll.load(ErrorEvent.class);
        assertNotNull(events);
        assertEquals(1, events.size());
        ErrorEvent savedEvent = (ErrorEvent) events.get(0);
        assertNull(savedEvent.getAwsId());

        doAll.delete();
    }

    @Test
    void testOnlyKin() {
        ErrorEvent event = new ErrorEvent("user", "jobId", ErrorEvent.ErrorType.FILE_ALREADY_DELETED,
                "File Deleted");
        logManager = new LogManager(sqlEventLogger, kinesisEventLogger);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                ((ErrorEvent)args[0]).setAwsId("aws1111");
                return null; // void method, so return null
            }
        }).when(kinesisEventLogger).log(event);
        logManager.log(LogManager.LogType.KINESIS, event);
        assertEquals("aws1111", event.getAwsId());
        List<LoggableEvent> events = doAll.load(ErrorEvent.class);
        assertNotNull(events);
        assertNull(event.getId());
        assertEquals(0, events.size());
    }
}