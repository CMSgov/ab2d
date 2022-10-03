package gov.cms.ab2d.eventlogger.eventloggers.kinesis;

import com.amazonaws.ResponseMetadata;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.eventlogger.AB2DLocalstackContainer;
import gov.cms.ab2d.eventlogger.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.AB2DSQSMockConfig;
import gov.cms.ab2d.eventlogger.SpringBootApp;
import gov.cms.ab2d.eventclient.events.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static gov.cms.ab2d.eventlogger.eventloggers.kinesis.KinesisEventProcessor.getJsonString;
import static gov.cms.ab2d.eventlogger.utils.UtilMethods.camelCaseToUnderscore;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
@Import(AB2DSQSMockConfig.class)
class KinesisEventLoggerTest {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private Ab2dEnvironment environment;

    @Value("${eventlogger.kinesis.enabled}")
    private KinesisMode kinesisEnabled;

    @Value("${eventlogger.kinesis.stream.prefix:}")
    private String streamId;

    @Autowired
    private KinesisConfig config;

    @Mock
    private AmazonKinesisFirehose firehose;

    private KinesisEventLogger logger;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        logger = new KinesisEventLogger(config, firehose, environment, kinesisEnabled, streamId);
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
    void sendEvent() {
        ErrorEvent e = new ErrorEvent();
        e.setDescription("Test Error 2");
        e.setErrorType(ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT);
        e.setId(1L);
        e.setJobId("JOB123");
        e.setTimeOfEvent(OffsetDateTime.now());
        e.setOrganization("ME");
        logger.log(e, true);
        assertNotNull(e.getAwsId());
    }

    @Test
    void sendOnlyBeneSearch() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime now2 = OffsetDateTime.now();
        BeneficiarySearchEvent se = new BeneficiarySearchEvent("laila", "job1", "contract1",
                now, now2, 1L, "SUCCESS");
        assertEquals(1, se.getBeneId());
        assertEquals("SUCCESS", se.getResponse());
        assertEquals("job1", se.getJobId());
        assertEquals("contract1", se.getContractNum());
        assertEquals(now, se.getTimeOfEvent());
        assertEquals(now2, se.getResponseDate());
    }

    @DisplayName("Kinesis parallel message sending")
    @Test
    void testThreadPool() throws InterruptedException {
        ErrorEvent e = new ErrorEvent();
        e.setDescription("Test Error 2");
        e.setErrorType(ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT);
        e.setId(1L);
        e.setJobId("JOB123");
        e.setTimeOfEvent(OffsetDateTime.now());
        e.setOrganization("ME");
        for (int i = 0; i < 20; i++) {
            logger.log(e, false);
        }
        while (!logger.isFinished()) {
            Thread.sleep(1000);
        }
        assertNotNull(e.getAwsId());
    }

    @DisplayName("Kinesis mode toggles logging")
    @Test
    void testLocal() {
        ReflectionTestUtils.setField(logger, "kinesisEnabled", KinesisMode.NONE);
        ErrorEvent e = new ErrorEvent();
        e.setDescription("Test Error");
        e.setErrorType(ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT);
        e.setId(1L);
        e.setJobId("JOB123");
        e.setTimeOfEvent(OffsetDateTime.now());
        e.setOrganization("ME");

        e.setAwsId("BOGUS");
        logger.log(e, true);
        String env = e.getEnvironment();
        // Since we never logged, aws id was not reset
        assertEquals("BOGUS", e.getAwsId());
        ReflectionTestUtils.setField(logger, "kinesisEnabled", KinesisMode.SEND_EVENTS);
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
        event = new ContractSearchEvent();
        assertEquals("contract_search_event", camelCaseToUnderscore(event.getClass().getSimpleName()));
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
    void testJsonConversion() throws IOException {
        OffsetDateTime now = OffsetDateTime.now();
        ErrorEvent e = new ErrorEvent();
        e.setDescription("Test Error");
        e.setErrorType(ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT);
        e.setId(1L);
        e.setJobId("JOB123");
        e.setTimeOfEvent(now);
        e.setOrganization("ME");
        e.setAwsId("BOGUS");
        e.setEnvironment(Ab2dEnvironment.DEV);

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = getJsonString(e);
        JsonNode node = mapper.readTree(jsonString);
        assertEquals("Test Error", node.get("description").asText());
        assertEquals(1, node.get("id").asInt());
        assertEquals("JOB123", node.get("job_id").asText());
        assertEquals("UNAUTHORIZED_CONTRACT", node.get("error_type").asText());
        assertEquals("ME", node.get("organization").asText());
        assertEquals("BOGUS", node.get("aws_id").asText());
        assertEquals("ab2d-dev", node.get("environment").asText());
        String dateString = node.get("time_of_event").asText();
        assertNotNull(dateString);
        assertTrue(dateString.contains("Z"));
        OffsetDateTime dateObj = OffsetDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
        assertEquals(now.toEpochSecond(), dateObj.toEpochSecond());
    }

    @DisplayName("Block client ids from being logged to Kinesis")
    @Test
    void blockClientIds() {

        FauxKinesisFirehose firehose = new FauxKinesisFirehose();
        KinesisEventLogger logger = new KinesisEventLogger(config, firehose, environment, kinesisEnabled, streamId);

        ErrorEvent e = new ErrorEvent();
        e.setDescription("Test Error 2");
        e.setErrorType(ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT);
        e.setId(1L);
        e.setJobId("JOB123");
        e.setTimeOfEvent(OffsetDateTime.now());
        e.setOrganization("0123");
        logger.log(e, true);

        try {
            byte[] array = firehose.latestRecord.getData().array();

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> event = mapper.readValue(new String(array), new TypeReference<>() {});

            assertTrue(event.containsKey("organization"));
            assertNull(event.get("organization"));
        } catch (IOException exception) {
            fail(exception);
        }
    }
}