package gov.cms.ab2d.eventlogger.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.eventclient.events.ApiRequestEvent;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.eventclient.events.ErrorEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventclient.messages.AlertSQSMessage;
import gov.cms.ab2d.eventclient.messages.GeneralSQSMessage;
import gov.cms.ab2d.eventclient.messages.KinesisSQSMessage;
import gov.cms.ab2d.eventclient.messages.LogAndTraceSQSMessage;
import gov.cms.ab2d.eventclient.messages.SQSMessages;
import gov.cms.ab2d.eventclient.messages.SlackSQSMessage;
import gov.cms.ab2d.eventclient.messages.TraceAndAlertSQSMessage;
import gov.cms.ab2d.eventclient.messages.TraceSQSMessage;
import gov.cms.ab2d.eventlogger.LogManager;


import gov.cms.ab2d.eventlogger.api.EventsListener;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
@EnableAutoConfiguration()
public class SendAndReceiveSqsEventTest {

    static {
        System.setProperty("spring.liquibase.enabled", "false");
        System.setProperty("feature.sqs.enabled", "true");
    }

    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();
    @Container
    private static final AB2DLocalstackContainer LOCALSTACK_CONTAINER = new AB2DLocalstackContainer();

    @Autowired
    private SQSEventClient sendSQSEvent;

    @Autowired
    private SqsAsyncClient amazonSQS;

    @MockBean
    private LogManager logManager;

    @Autowired
    private EventsListener eventListener;

    @Test
    void testQueueUrl() {
         String sqs = "ab2d-dev-events-sqs";
        String url = amazonSQS.getQueueUrl(GetQueueUrlRequest.builder().queueName(sqs).build()).join().queueUrl();
        Assertions.assertTrue(url.contains(sqs));
    }

    @Test
    void testSendAndReceiveMessages() {
        final ArgumentCaptor<LoggableEvent> captor = ArgumentCaptor.forClass(LoggableEvent.class);
        ApiRequestEvent sentApiRequestEvent = new ApiRequestEvent("organization", "jobId", "url", "ipAddress", "token", "requestId");
        ApiResponseEvent sentApiResponseEvent = new ApiResponseEvent("organization", "jobId", HttpStatus.I_AM_A_TEAPOT, "ipAddress", "token", "requestId");

        sendSQSEvent.sendLogs(sentApiRequestEvent);
        sendSQSEvent.sendLogs(sentApiResponseEvent);

        //timeout needed because the sqs listener (that uses logManager) is a separate process.
        verify(logManager, timeout(1000).times(2)).log(captor.capture());

        List<LoggableEvent> loggedApiRequestEvent = captor.getAllValues();
        Assertions.assertEquals(sentApiRequestEvent, loggedApiRequestEvent.get(0));
        Assertions.assertEquals(sentApiResponseEvent, loggedApiRequestEvent.get(1));
        Assertions.assertEquals(ApiRequestEvent.class, loggedApiRequestEvent.get(0).getClass());
        Assertions.assertEquals(ApiResponseEvent.class, loggedApiRequestEvent.get(1).getClass());
    }

    @Test
    void testNonVerifiedObject() throws JsonProcessingException {
        NonVerifiedSQSMessages fakeObject = new NonVerifiedSQSMessages();

        eventListener.processEvents(fakeObject);

        //timeout needed because the sqs listener (that uses logManager) is a separate process.
        verify(logManager, never()).log(any(LoggableEvent.class));
    }

    @Test
    void testEventListener() {
        ErrorEvent event = new ErrorEvent("user", "jobId", ErrorEvent.ErrorType.FILE_ALREADY_DELETED,
                "File Deleted");

        SQSMessages sqsMessages = new GeneralSQSMessage(event);
        eventListener.processEvents(sqsMessages);
        sqsMessages = new AlertSQSMessage(event.getDescription(), Collections.singletonList(Ab2dEnvironment.LOCAL));
        eventListener.processEvents(sqsMessages);
        sqsMessages = new TraceSQSMessage(event.getDescription(), Collections.singletonList(Ab2dEnvironment.LOCAL));
        eventListener.processEvents(sqsMessages);
        sqsMessages = new TraceAndAlertSQSMessage(event, Collections.singletonList(Ab2dEnvironment.LOCAL));
        eventListener.processEvents(sqsMessages);
        sqsMessages = new LogAndTraceSQSMessage(event, Collections.singletonList(Ab2dEnvironment.LOCAL));
        eventListener.processEvents(sqsMessages);
        sqsMessages = new SlackSQSMessage(event);
        eventListener.processEvents(sqsMessages);
        sqsMessages = new KinesisSQSMessage(event);
        eventListener.processEvents(sqsMessages);

        //timeout needed because the sqs listener (that uses logManager) is a separate process.
        verify(logManager, times(1)).log(any(LoggableEvent.class));
        verify(logManager, times(1)).trace(anyString(), anyList());
        verify(logManager, times(1)).alert(anyString(), anyList());
        verify(logManager, times(2)).log(any(LogManager.LogType.class), any(LoggableEvent.class));
        verify(logManager, times(1)).logAndAlert(any(LoggableEvent.class), anyList());
        verify(logManager, times(1)).logAndTrace(any(LoggableEvent.class), anyList());

    }

    public class NonVerifiedSQSMessages extends SQSMessages {
        private LoggableEvent loggableEvent;

        public NonVerifiedSQSMessages() {
        }
    }

}
