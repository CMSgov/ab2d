package gov.cms.ab2d.eventclient.clients;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventclient.messages.AlertSQSMessage;
import gov.cms.ab2d.eventclient.messages.GeneralSQSMessage;
import gov.cms.ab2d.eventclient.messages.KinesisSQSMessage;
import gov.cms.ab2d.eventclient.messages.LogAndTraceSQSMessage;
import gov.cms.ab2d.eventclient.messages.SQSMessages;
import gov.cms.ab2d.eventclient.messages.SlackSQSMessage;
import gov.cms.ab2d.eventclient.messages.TraceAndAlertSQSMessage;
import gov.cms.ab2d.eventclient.messages.TraceSQSMessage;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;

@Slf4j
public class SQSEventClient implements EventClient {
    private final SqsAsyncClient amazonSQS;
    private final ObjectMapper mapper;

    private final String queueName;

    public SQSEventClient(SqsAsyncClient amazonSQS, ObjectMapper mapper, String queueName) {
        this.amazonSQS = amazonSQS;
        this.mapper = mapper;
        this.queueName = queueName;
    }

    @Override
    public void sendLogs(LoggableEvent requestEvent) {
        log.info("Send logs to queue: {}", queueName);
        GeneralSQSMessage sqsMessage = new GeneralSQSMessage(requestEvent);
        sendMessage(sqsMessage);
    }

    @Override
    public void alert(String message, List<Ab2dEnvironment> environments) {
        log.info("Send alert to queue: {}", queueName);
        AlertSQSMessage sqsMessage = new AlertSQSMessage(message, environments);
        sendMessage(sqsMessage);
    }

    /**
     * Alert only AB2D team via relevant loggers with a low priority alert
     *
     * @param message      message to provide
     * @param environments AB2D environments to alert on
     */
    @Override
    public void trace(String message, List<Ab2dEnvironment> environments) {
        TraceSQSMessage sqsMessage = new TraceSQSMessage(message, environments);
        sendMessage(sqsMessage);
    }

    /**
     * Log the event and alert to relevant alert loggers only in the provided execution environments.
     * The message published will be built using {@link LoggableEvent#asMessage()}
     *
     * @param event        event to log an alert for.
     * @param environments environments to log an alert for
     */
    @Override
    public void logAndAlert(LoggableEvent event, List<Ab2dEnvironment> environments) {
        TraceAndAlertSQSMessage sqsMessage = new TraceAndAlertSQSMessage(event, environments);
        sendMessage(sqsMessage);
    }

    /**
     * Log the event and provided traces to relevant trace loggers only in the provided execution environments.
     * The message published will be built using {@link LoggableEvent#asMessage()}
     *
     * @param event        event to log an alert for.
     * @param environments environments to log an alert for
     */
    @Override
    public void logAndTrace(LoggableEvent event, List<Ab2dEnvironment> environments) {
        LogAndTraceSQSMessage sqsMessage = new LogAndTraceSQSMessage(event, environments);
        sendMessage(sqsMessage);
    }

    /**
     * Log an event without alerting
     *
     * @param type  type of event logger to use
     * @param event event to log
     */
    @Override
    public void log(LogType type, LoggableEvent event) {
        switch (type) {
            case SQL:
                sendMessage(new SlackSQSMessage(event));
                break;
            case KINESIS:
                sendMessage(new KinesisSQSMessage(event));
                break;
            default:
                break;
        }
    }

    public void sendMessage(SQSMessages message) {
        try {
            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build();
            String queueUrl = amazonSQS.getQueueUrl(getQueueRequest).join().queueUrl();
            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(mapper.writeValueAsString(message))
                    .build();

            amazonSQS.sendMessage(sendMessageRequest);

        } catch (JsonProcessingException | UnsupportedOperationException | SqsException e) {
            log.info(e.getMessage());
        }
    }
}
