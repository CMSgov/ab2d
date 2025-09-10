package gov.cms.ab2d.eventclient.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.eventclient.clients.EventClient;
import gov.cms.ab2d.eventclient.clients.SQSConfig;
import gov.cms.ab2d.eventclient.events.ApiRequestEvent;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.eventclient.events.ErrorEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;

import java.lang.UnsupportedOperationException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {"spring.liquibase.enabled=false"})
@ExtendWith(OutputCaptureExtension.class)
@Testcontainers
public class SendSqsEventTest {

    @Container
    private static final AB2DLocalstackContainer LOCALSTACK_CONTAINER = new AB2DLocalstackContainer();
    public static final String LOCAL_EVENTS_SQS = "local-events-sqs";
    public static final String DEV_EVENTS_SQS = "ab2d-dev-events-sqs";

    @Autowired
    private SqsAsyncClient amazonSQS;

    private final ObjectMapper mapper = SQSConfig.objectMapper();

    @Test
    void testQueueUrl() {
        String url = amazonSQS.getQueueUrl(GetQueueUrlRequest.builder().queueName(LOCAL_EVENTS_SQS).build()).join().queueUrl();
        assertTrue(url.contains(LOCAL_EVENTS_SQS));
    }

    @Test
    void testSendMessages() throws JsonProcessingException {
        SqsAsyncClient amazonSQSSpy = Mockito.spy(amazonSQS);
        SQSEventClient sqsEventClient = new SQSEventClient(amazonSQSSpy, mapper, LOCAL_EVENTS_SQS);

        final ArgumentCaptor<LoggableEvent> captor = ArgumentCaptor.forClass(LoggableEvent.class);
        ApiRequestEvent sentApiRequestEvent = new ApiRequestEvent("organization", "jobId", "url", "ipAddress", "token", "requestId");
        ApiResponseEvent sentApiResponseEvent = new ApiResponseEvent("organization", "jobId", HttpStatus.I_AM_A_TEAPOT, "ipAddress", "token", "requestId");

        sqsEventClient.sendLogs(sentApiRequestEvent);
        sqsEventClient.sendLogs(sentApiResponseEvent);

        Mockito.verify(amazonSQSSpy, timeout(1000).times(2)).sendMessage(any(SendMessageRequest.class));

        List<Message> message1 = amazonSQS.receiveMessage(ReceiveMessageRequest.builder().queueUrl(LOCAL_EVENTS_SQS).build()).join().messages();
        List<Message> message2 = amazonSQS.receiveMessage(ReceiveMessageRequest.builder().queueUrl(LOCAL_EVENTS_SQS).build()).join().messages();

        assertTrue(message1.get(0).body().contains(mapper.writeValueAsString(sentApiRequestEvent)));
        assertTrue(message2.get(0).body().contains(mapper.writeValueAsString(sentApiResponseEvent)));
    }

    @Test
    void testSendMessagesDifferentQueue() throws JsonProcessingException {
        amazonSQS.createQueue( CreateQueueRequest.builder()
                .queueName("ab2d-dev-events-sqs")
                .build());
        SqsAsyncClient amazonSQSSpy = Mockito.spy(amazonSQS);
        SQSEventClient sqsEventClient = new SQSEventClient(amazonSQSSpy, mapper, DEV_EVENTS_SQS);

        final ArgumentCaptor<LoggableEvent> captor = ArgumentCaptor.forClass(LoggableEvent.class);
        ApiRequestEvent sentApiRequestEvent = new ApiRequestEvent("organization", "jobId", "url", "ipAddress", "token", "requestId");
        ApiResponseEvent sentApiResponseEvent = new ApiResponseEvent("organization", "jobId", HttpStatus.I_AM_A_TEAPOT, "ipAddress", "token", "requestId");

        sqsEventClient.sendLogs(sentApiRequestEvent);
        sqsEventClient.sendLogs(sentApiResponseEvent);

        Mockito.verify(amazonSQSSpy, timeout(1000).times(2)).sendMessage(any(SendMessageRequest.class));

        List<Message> message1 = amazonSQS.receiveMessage(ReceiveMessageRequest.builder().queueUrl(DEV_EVENTS_SQS).build()).join().messages();
        List<Message> message2 = amazonSQS.receiveMessage(ReceiveMessageRequest.builder().queueUrl(DEV_EVENTS_SQS).build()).join().messages();

        assertTrue(message1.get(0).body().contains(mapper.writeValueAsString(sentApiRequestEvent)));
        assertTrue(message2.get(0).body().contains(mapper.writeValueAsString(sentApiResponseEvent)));
    }

    @Test
    void logWithSQS() {
        SqsAsyncClient amazonSQSSpy = Mockito.spy(amazonSQS);
        SQSEventClient sqsEventClient = new SQSEventClient(amazonSQSSpy, mapper, LOCAL_EVENTS_SQS);

        ErrorEvent event = new ErrorEvent("user", "jobId", ErrorEvent.ErrorType.FILE_ALREADY_DELETED,
                "File Deleted");

        ArrayList<Ab2dEnvironment> enviroments = new ArrayList<>();
        enviroments.add(Ab2dEnvironment.LOCAL);
        sqsEventClient.sendLogs(event);
        sqsEventClient.trace(event.getDescription(), enviroments);
        sqsEventClient.alert(event.getDescription(), enviroments);
        sqsEventClient.log(EventClient.LogType.SQL, event);
        sqsEventClient.log(EventClient.LogType.KINESIS, event);
        sqsEventClient.logAndAlert(event, enviroments);
        sqsEventClient.logAndTrace(event, enviroments);

        Mockito.verify(amazonSQSSpy, timeout(1000).times(7)).sendMessage(any(SendMessageRequest.class));

        List<Message> message = amazonSQS.receiveMessage(ReceiveMessageRequest.builder().queueUrl(LOCAL_EVENTS_SQS).build()).join().messages();
        assertTrue(message.get(0).body().contains("GeneralSQSMessage"));

        message = amazonSQS.receiveMessage(ReceiveMessageRequest.builder().queueUrl(LOCAL_EVENTS_SQS).build()).join().messages();
        assertTrue(message.get(0).body().contains("TraceSQSMessage"));

        message = amazonSQS.receiveMessage(ReceiveMessageRequest.builder().queueUrl(LOCAL_EVENTS_SQS).build()).join().messages();
        assertTrue(message.get(0).body().contains("AlertSQSMessage"));

        message = amazonSQS.receiveMessage(ReceiveMessageRequest.builder().queueUrl(LOCAL_EVENTS_SQS).build()).join().messages();
        assertTrue(message.get(0).body().contains("SlackSQSMessage"));

        message = amazonSQS.receiveMessage(ReceiveMessageRequest.builder().queueUrl(LOCAL_EVENTS_SQS).build()).join().messages();
        assertTrue(message.get(0).body().contains("KinesisSQSMessage"));

        message = amazonSQS.receiveMessage(ReceiveMessageRequest.builder().queueUrl(LOCAL_EVENTS_SQS).build()).join().messages();
        assertTrue(message.get(0).body().contains("TraceAndAlertSQSMessage"));

        message = amazonSQS.receiveMessage(ReceiveMessageRequest.builder().queueUrl(LOCAL_EVENTS_SQS).build()).join().messages();
        assertTrue(message.get(0).body().contains("LogAndTraceSQSMessage"));
    }

    @Test
    void testFailedMapping(CapturedOutput output) {
        SqsAsyncClient amazonSQSMock = Mockito.mock(SqsAsyncClient.class);
        GetQueueUrlResponse queueURL = Mockito.mock(GetQueueUrlResponse.class);

        when(amazonSQSMock.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(CompletableFuture.completedFuture(queueURL));
        when(queueURL.queueUrl()).thenReturn("http://localhost:4321");
        when(amazonSQSMock.sendMessage(any(SendMessageRequest.class))).thenThrow(new UnsupportedOperationException("foobar"));
        SQSEventClient sqsEventClient = new SQSEventClient(amazonSQSMock, mapper, LOCAL_EVENTS_SQS);

        ApiRequestEvent sentApiRequestEvent = new ApiRequestEvent("organization", "jobId", "url", "ipAddress", "token", "requestId");

        sqsEventClient.sendLogs(sentApiRequestEvent);
        Assertions.assertTrue(output.getOut().contains("foobar"));
    }

    @Test
    void testAB2DEEnvironment() {
        String url = "https://sqs.us-east-1.amazonaws.com/123456789/ab2d-dev-events-sqs";

        new SQSConfig("us-east-1", url, null);
        assertEquals("ab2d-dev-events-sqs", System.getProperty("sqs.queue-name"));

    }
}
