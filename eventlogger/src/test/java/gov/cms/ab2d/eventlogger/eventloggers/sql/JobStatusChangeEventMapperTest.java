package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.SpringBootApp;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.eventlogger.events.JobStatusChangeEvent;
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
class JobStatusChangeEventMapperTest {
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
                new JobStatusChangeEventMapper(null).log(new FileEvent()));
    }

    @Test
    void log() {
        JobStatusChangeEvent jsce = new JobStatusChangeEvent("laila", "job123", "IN_PROGRESS",
                "FAILED", "Description");
        sqlEventLogger.log(jsce);
        long id = jsce.getId();
        OffsetDateTime val = jsce.getTimeOfEvent();
        List<LoggableEvent> events = loadObjects.loadAllJobStatusChangeEvent();
        assertEquals(1, events.size());
        JobStatusChangeEvent event = (JobStatusChangeEvent) events.get(0);
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), jsce.getId());
        assertEquals("laila", event.getUser());
        assertEquals("job123", event.getJobId());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        assertEquals("FAILED", event.getNewStatus());
        assertEquals("IN_PROGRESS", event.getOldStatus());
        assertEquals("Description", event.getDescription());
        deleteObjects.deleteAllJobStatusChangeEvent();
        events = loadObjects.loadAllJobStatusChangeEvent();
        assertEquals(0, events.size());
    }
}