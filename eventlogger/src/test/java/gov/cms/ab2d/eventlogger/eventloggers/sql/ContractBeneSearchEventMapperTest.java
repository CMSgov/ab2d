package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.SpringBootApp;
import gov.cms.ab2d.eventlogger.events.ContractBeneSearchEvent;
import gov.cms.ab2d.eventlogger.reports.sql.LoadObjects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
class ContractBeneSearchEventMapperTest {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    SqlEventLogger sqlEventLogger;

    @Autowired
    LoadObjects loadObjects;

    @Test
    void log() {
        ContractBeneSearchEvent cbse = new ContractBeneSearchEvent(
                "laila", "jobIdVal", "Contract123", 100, 95, 3, 2);
        sqlEventLogger.log(cbse);
        long id = cbse.getId();
        OffsetDateTime val = cbse.getTimeOfEvent();
        List<LoggableEvent> events = loadObjects.loadAllContractBeneSearchEvent();
        assertEquals(1, events.size());
        ContractBeneSearchEvent event = (ContractBeneSearchEvent) events.get(0);
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), cbse.getId());
        assertEquals("laila", event.getUser());
        assertEquals("jobIdVal", event.getJobId());
        assertEquals("Contract123", event.getContractNumber());
        assertEquals(100, event.getNumInContract());
        assertEquals(95, event.getNumSearched());
        assertEquals(3, event.getNumOptedOut());
        assertEquals(2, event.getNumErrors());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
    }
}