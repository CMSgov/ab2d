package gov.cms.ab2d.eventlogger.eventloggers.kinesis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import gov.cms.ab2d.eventlogger.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.SpringBootApp;
import gov.cms.ab2d.eventlogger.events.BeneficiarySearchEvent;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
class KinesisEventLoggerTest {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private KinesisEventLogger logger;

    /*
    @Test
    public void sendEvent() {
        ErrorEvent e = new ErrorEvent();
        e.setDescription("Test Error 2");
        e.setErrorType(ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT);
        e.setId(1L);
        e.setJobId("JOB123");
        e.setTimeOfEvent(OffsetDateTime.now());
        e.setUser("ME");
        logger.log(e);
        assertNotNull(e.getAwsId());
    }
    */

    @Test
    public void sendOnlyBeneSearch() throws JsonProcessingException, JSONException {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime now2 = OffsetDateTime.now();
        BeneficiarySearchEvent se = new BeneficiarySearchEvent("laila", "job1", "contract1",
                now, now2, "bene1", "SUCCESS");
        assertEquals("bene1", se.getBeneId());
        assertEquals("SUCCESS", se.getResponse());
        assertEquals("job1", se.getJobId());
        assertEquals("contract1", se.getContractNum());
        assertEquals(now, se.getTimeOfEvent());
        assertEquals(now2, se.getResponseDate());
        String json = KinesisEventLogger.getJsonString(se);
        System.out.println(json);
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
        BeneficiarySearchEvent resultBene = mapper.readValue(json, BeneficiarySearchEvent.class);
        assertEquals("bene1", resultBene.getBeneId());
        assertEquals("SUCCESS", resultBene.getResponse());
        assertEquals("job1", resultBene.getJobId());
        assertEquals("contract1", resultBene.getContractNum());
        assertEquals(now.getNano(), resultBene.getTimeOfEvent().getNano());
        assertEquals(now2.getNano(), resultBene.getResponseDate().getNano());
    }
}