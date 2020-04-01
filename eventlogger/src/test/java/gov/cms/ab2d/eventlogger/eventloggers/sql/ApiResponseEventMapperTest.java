package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.SpringBootApp;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
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
class ApiResponseEventMapperTest {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    SqlEventLogger sqlEventLogger;

    @Autowired
    LoadObjects loadObjects;

    @Test
    void exceptionTests() {
        assertThrows(EventLoggingException.class, () ->
                new ApiResponseEventMapper(null).log(new ErrorEvent()));
    }

    @Test
    void log() {
        ApiResponseEvent jsce = new ApiResponseEvent("laila", "job123", 404,
                "Not Found", "Description", "123");
        sqlEventLogger.log(jsce);
        long id = jsce.getId();
        OffsetDateTime val = jsce.getTimeOfEvent();
        List<LoggableEvent> events = loadObjects.loadAllApiResponseEvent();
        assertEquals(1, events.size());
        ApiResponseEvent event = (ApiResponseEvent) events.get(0);
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), jsce.getId());
        assertEquals("laila", event.getUser());
        assertEquals("job123", event.getJobId());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        assertEquals("Description", event.getDescription());
        assertEquals("Not Found", event.getResponseString());
        assertEquals(404, event.getResponseCode());
        assertEquals("123", event.getRequestId());
    }
}