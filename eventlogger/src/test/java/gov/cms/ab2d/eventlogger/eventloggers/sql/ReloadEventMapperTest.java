package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.SpringBootApp;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.eventlogger.reports.sql.DeleteObjects;
import gov.cms.ab2d.eventlogger.reports.sql.LoadObjects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
class ReloadEventMapperTest {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    SqlEventLogger sqlEventLogger;

    @Autowired
    LoadObjects loadObjects;

    @Autowired
    DeleteObjects deleteObjects;

    @Test
    void exceptionTests() {
        assertThrows(EventLoggingException.class, () ->
                new ReloadEventMapper(null).log(new ErrorEvent()));
    }

    @Test
    void log() {
        ReloadEvent cbse = new ReloadEvent(null, ReloadEvent.FileType.CONTRACT_MAPPING,
                "filename", 10);
        sqlEventLogger.log(cbse);
        long id = cbse.getId();
        OffsetDateTime val = cbse.getTimeOfEvent();
        List<LoggableEvent> events = loadObjects.loadAllReloadEvent();
        assertEquals(1, events.size());
        ReloadEvent event = (ReloadEvent) events.get(0);
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), cbse.getId());
        assertNull(event.getUser());
        assertNull(event.getJobId());
        assertEquals("filename", event.getFileName());
        assertEquals(10, event.getNumberLoaded());
        assertEquals(ReloadEvent.FileType.CONTRACT_MAPPING, event.getFileType());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        deleteObjects.deleteAllReloadEvent();
        events = loadObjects.loadAllReloadEvent();
        assertEquals(0, events.size());
    }
}