package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.SpringBootApp;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;
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
class ApiRequestEventMapperTest {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    SqlEventLogger sqlEventLogger;

    @Autowired
    LoadObjects loadObjects;

    @Test
    void log() {
        ApiRequestEvent jsce = new ApiRequestEvent("laila", "job123", "http://localhost",
                "127.0.0.1", "hash", "123");
        sqlEventLogger.log(jsce);
        long id = jsce.getId();
        OffsetDateTime val = jsce.getTimeOfEvent();
        List<LoggableEvent> events = loadObjects.loadAllApiRequestEvent();
        assertEquals(1, events.size());
        ApiRequestEvent event = (ApiRequestEvent) events.get(0);
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), jsce.getId());
        assertEquals("laila", event.getUser());
        assertEquals("job123", event.getJobId());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        assertEquals("http://localhost", event.getUrl());
        assertEquals("127.0.0.1", event.getIpAddress());
        assertEquals("hash", event.getTokenHash());
        assertEquals("123", event.getRequestId());
    }
}