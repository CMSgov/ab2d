package gov.cms.ab2d.metrics;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.ab2d.testutils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

class InvokeTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JodaModule())
            .registerModule(new JavaTimeModule());

    @BeforeEach
    public void before() {
        setEnv("IS_LOCALSTACK", "true");
    }

    @Test
    void invokeTestOk() throws Exception {
        invoke("ab2d-dev-test", "OK", "2022-09-14T19:03:51.523+0100");
    }

    @Test
    void invokeTestAlarm() throws Exception {
        invoke("ab2d-dev-test", "ALARM", "2022-09-14T19:03:51.523+0100");
    }

    @Test
    void invokeTestAlarmNameFail() throws Exception {
        invoke(null, "OK", "2022-09-14T19:03:51.523+0100");
    }

    @Test
    void invokeTest() throws Exception {
        invoke("test", "OK", "2022-09-14T19:03:51.523+0100");
    }

    @Test
    void invokeTestFail() throws Exception {
        invoke("test", null, null);
    }


    private void invoke(String alarmName, String state, String time) throws IllegalAccessException, JsonProcessingException {
        MetricAlarm metricAlarm = new MetricAlarm();
        metricAlarm.setAlarmName(alarmName);
        metricAlarm.setStateChangeTime(time);
        metricAlarm.setNewStateValue(state);
        Trigger trigger = new Trigger();
        trigger.setNamespace("test");
        metricAlarm.setTrigger(trigger);
        SNSEvent event = new SNSEvent();
        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setMessage(objectMapper.writeValueAsString(metricAlarm));
        record.setSns(sns);
        event.setRecords(List.of(record));
        Context context = new TestContext();
        Field sqs = ReflectionUtils.findFields(CloudwatchEventHandler.class, (f) -> f.getName()
                        .equals("amazonSQS"), ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Field not found"));
        CloudwatchEventHandler handler = new CloudwatchEventHandler();
        sqs.setAccessible(true);
        AmazonSQSClient mockedSQS = Mockito.mock(AmazonSQSClient.class);
        sqs.set(handler, mockedSQS);
        Mockito.when(mockedSQS.getQueueUrl(anyString()))
                .thenReturn(new GetQueueUrlResult());

        String result = handler.handleRequest(event, context);
        assertEquals("OK", result);
    }

    @Test
    void setupTest() throws NoSuchMethodException {
        CloudwatchEventHandler handler = new CloudwatchEventHandler();
        Method setup = ReflectionUtils.makeAccessible(CloudwatchEventHandler.class.getDeclaredMethod("setup"));
        assertDoesNotThrow(() -> {
            ReflectionUtils.invokeMethod(setup, handler);
        });
    }

    @Test
    void setupTestLocalstack() throws NoSuchMethodException {
        setEnv("IS_LOCALSTACK", "true");
        assertEquals("true", System.getenv("IS_LOCALSTACK"));
        CloudwatchEventHandler handler = new CloudwatchEventHandler();
        Method setup = ReflectionUtils.makeAccessible(CloudwatchEventHandler.class.getDeclaredMethod("setup"));
        assertDoesNotThrow(() -> {
            ReflectionUtils.invokeMethod(setup, handler);
        });
    }

    public static void setEnv(String key, String value) {
        try {
            Map<String, String> env = System.getenv();
            Class<?> cl = env.getClass();
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            writableEnv.put(key, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set environment variable", e);
        }
    }


}
