package gov.cms.ab2d.eventlogger.eventloggers.kinesis;

import com.amazonaws.ResponseMetadata;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.ab2d.eventlogger.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.SpringBootApp;
import gov.cms.ab2d.eventlogger.events.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static gov.cms.ab2d.eventlogger.utils.UtilMethods.camelCaseToUnderscore;
import static gov.cms.ab2d.eventlogger.eventloggers.kinesis.KinesisEventProcessor.getJsonString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
class KinesisEventLoggerTest {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Value("${execution.env}")
    private String appEnv;

    @Value("${eventlogger.kinesis.enabled}")
    private boolean kinesisEnabled;

    @Value("${eventlogger.kinesis.stream.prefix:}")
    private String streamId;

    @Autowired
    private KinesisConfig config;

    @Mock
    private AmazonKinesisFirehose firehose;

    private KinesisEventLogger logger;

    @BeforeEach
    void init() {
        logger = new KinesisEventLogger(config, firehose, appEnv, kinesisEnabled, streamId);
        ReflectionTestUtils.setField(logger, "appEnv", "dev");
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
        for (int i = 0; i < 20; i++) {
            logger.log(e, false);
        }
        while (!logger.isFinished()) {
            Thread.sleep(1000);
        }
        assertNotNull(e.getAwsId());
    }

    @Test
    public void testLocal() {
        ReflectionTestUtils.setField(logger, "appEnv", "local");
        ErrorEvent e = new ErrorEvent();
        e.setDescription("Test Error");
        e.setErrorType(ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT);
        e.setId(1L);
        e.setJobId("JOB123");
        e.setTimeOfEvent(OffsetDateTime.now());
        e.setUser("ME");

        e.setAwsId("BOGUS");
        logger.log(e, true);
        String env = e.getEnvironment();
        // Since we never logged, aws id was not reset
        assertEquals("BOGUS", e.getAwsId());
        ReflectionTestUtils.setField(logger, "appEnv", "dev");
        logger.log(e, true);
        assertTrue(e.getAwsId() == null || !e.getAwsId().equalsIgnoreCase("BOGUS"));
    }

    @Test
    void camelCaseConvertestTest() {
        LoggableEvent event = new ApiRequestEvent();
        assertEquals("api_request_event", camelCaseToUnderscore(event.getClass().getSimpleName()));
        event = new ApiResponseEvent();
        assertEquals("api_response_event", camelCaseToUnderscore(event.getClass().getSimpleName()));
        event = new BeneficiarySearchEvent();
        assertEquals("beneficiary_search_event", camelCaseToUnderscore(event.getClass().getSimpleName()));
        event = new ContractBeneSearchEvent();
        assertEquals("contract_bene_search_event", camelCaseToUnderscore(event.getClass().getSimpleName()));
        event = new ErrorEvent();
        assertEquals("error_event", camelCaseToUnderscore(event.getClass().getSimpleName()));
        event = new FileEvent();
        assertEquals("file_event", camelCaseToUnderscore(event.getClass().getSimpleName()));
        event = new JobStatusChangeEvent();
        assertEquals("job_status_change_event", camelCaseToUnderscore(event.getClass().getSimpleName()));
        event = new ReloadEvent();
        assertEquals("reload_event", camelCaseToUnderscore(event.getClass().getSimpleName()));
    }

    @Test
    void testJsonConversion() throws JSONException {
        OffsetDateTime now = OffsetDateTime.now();
        ErrorEvent e = new ErrorEvent();
        e.setDescription("Test Error");
        e.setErrorType(ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT);
        e.setId(1L);
        e.setJobId("JOB123");
        e.setTimeOfEvent(now);
        e.setUser("ME");
        e.setAwsId("BOGUS");
        e.setEnvironment("dev");

        String jsonString = getJsonString(e);
        JSONObject jsonObj = new JSONObject(jsonString);
        assertEquals("Test Error", jsonObj.getString("description"));
        assertEquals(1, jsonObj.getInt("id"));
        assertEquals("JOB123", jsonObj.getString("job_id"));
        assertEquals("UNAUTHORIZED_CONTRACT", jsonObj.getString("error_type"));
        assertEquals(DigestUtils.sha1Hex("ME").toUpperCase(), jsonObj.getString("user"));
        assertEquals("BOGUS", jsonObj.getString("aws_id"));
        assertEquals("dev", jsonObj.getString("environment"));
        String dateString = jsonObj.getString("time_of_event");
        assertNotNull(dateString);
        assertTrue(dateString.contains("Z"));
        OffsetDateTime dateObj = OffsetDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
        assertEquals(now.toEpochSecond(), dateObj.toEpochSecond());
    }
}