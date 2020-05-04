package gov.cms.ab2d.eventlogger.eventloggers.kinesis;

import com.amazonaws.ResponseMetadata;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
class KinesisEventLoggerTest {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private KinesisConfig config;

    @Mock
    private AmazonKinesisFirehose firehose;

    private KinesisEventLogger logger;

    @BeforeEach
    void init() {
        logger = new KinesisEventLogger(config, firehose);
        doReturn(generateRandomResult()).when(firehose).putRecord(any());
    }

    private PutRecordResult generateRandomResult() {
        PutRecordResult putRecordResult = new PutRecordResult();
        Map<String, String> values = new HashMap<>();
        values.put("AWS_REQUEST_ID", UUID.randomUUID().toString());
        ResponseMetadata data = new ResponseMetadata(values);
        putRecordResult.setSdkResponseMetadata(data);
        return putRecordResult;
    }

    @Test
    public void sendEvent() {
        ErrorEvent e = new ErrorEvent();
        e.setDescription("Test Error 2");
        e.setErrorType(ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT);
        e.setId(1L);
        e.setJobId("JOB123");
        e.setTimeOfEvent(OffsetDateTime.now());
        e.setUser("ME");
        logger.log(e, true);
        assertNotNull(e.getAwsId());
    }

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
        String json = KinesisEventProcessor.getJsonString(se);
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

    @Test
    public void testThreadPool() throws InterruptedException {
        ErrorEvent e = new ErrorEvent();
        e.setDescription("Test Error 2");
        e.setErrorType(ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT);
        e.setId(1L);
        e.setJobId("JOB123");
        e.setTimeOfEvent(OffsetDateTime.now());
        e.setUser("ME");
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        logger.log(e);
        while (!logger.isFinished()) {
            Thread.sleep(1000);
        }
        assertNotNull(e.getAwsId());
    }
}