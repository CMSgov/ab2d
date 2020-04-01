package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.SpringBootApp;
import gov.cms.ab2d.eventlogger.events.BeneficiaryReloadEvent;
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
class BeneficiaryReloadEventMapperTest {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    SqlEventLogger sqlEventLogger;

    @Autowired
    LoadObjects loadObjects;

    @Test
    void exceptionTests() {
        assertThrows(EventLoggingException.class, () ->
                new BeneficiaryReloadEventMapper(null).log(new ErrorEvent()));
    }

    @Test
    void log() {
        BeneficiaryReloadEvent cbse = new BeneficiaryReloadEvent(BeneficiaryReloadEvent.FileType.CONTRACT_MAPPING,
                "filename", 10);
        sqlEventLogger.log(cbse);
        long id = cbse.getId();
        OffsetDateTime val = cbse.getTimeOfEvent();
        List<LoggableEvent> events = loadObjects.loadAllBeneficiaryReloadEvent();
        assertEquals(1, events.size());
        BeneficiaryReloadEvent event = (BeneficiaryReloadEvent) events.get(0);
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), cbse.getId());
        assertNull(event.getUser());
        assertNull(event.getJobId());
        assertEquals("filename", event.getFileName());
        assertEquals(10, event.getNumberLoaded());
        assertEquals(BeneficiaryReloadEvent.FileType.CONTRACT_MAPPING, event.getFileType());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
    }
}