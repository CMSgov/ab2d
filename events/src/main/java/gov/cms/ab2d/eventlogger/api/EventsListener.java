package gov.cms.ab2d.eventlogger.api;

import gov.cms.ab2d.eventclient.messages.AlertSQSMessage;
import gov.cms.ab2d.eventclient.messages.GeneralSQSMessage;
import gov.cms.ab2d.eventclient.messages.KinesisSQSMessage;
import gov.cms.ab2d.eventclient.messages.LogAndTraceSQSMessage;
import gov.cms.ab2d.eventclient.messages.SQSMessages;
import gov.cms.ab2d.eventclient.messages.SlackSQSMessage;
import gov.cms.ab2d.eventclient.messages.TraceAndAlertSQSMessage;
import gov.cms.ab2d.eventclient.messages.TraceSQSMessage;
import gov.cms.ab2d.eventlogger.LogManager;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode.ON_SUCCESS;

@Slf4j
@Service
@DependsOn({"SQSConfig"})
public class EventsListener {
    private final LogManager logManager;

    public EventsListener(LogManager logManager) {
        this.logManager = logManager;
    }

    @SqsListener(value = "${sqs.queue-name}")//, acknowledgementMode = ON_SUCCESS)
    @Async
    public void processEvents(SQSMessages sqsMessage) {
        try {
            log.info("EventsListener: Processing events from SQS: " + sqsMessage.getClass().getSimpleName());
            switch (sqsMessage.getClass().getSimpleName()) {
                case "GeneralSQSMessage" -> logManager.log(((GeneralSQSMessage) sqsMessage).getLoggableEvent());
                case "AlertSQSMessage" ->
                        logManager.alert(((AlertSQSMessage) sqsMessage).getMessage(), ((AlertSQSMessage) sqsMessage).getEnvironments());
                case "TraceSQSMessage" ->
                        logManager.trace(((TraceSQSMessage) sqsMessage).getMessage(), ((TraceSQSMessage) sqsMessage).getEnvironments());
                case "TraceAndAlertSQSMessage" ->
                        logManager.logAndAlert(((TraceAndAlertSQSMessage) sqsMessage).getLoggableEvent(), ((TraceAndAlertSQSMessage) sqsMessage).getEnvironments());
                case "LogAndTraceSQSMessage" ->
                        logManager.logAndTrace(((LogAndTraceSQSMessage) sqsMessage).getLoggableEvent(), ((LogAndTraceSQSMessage) sqsMessage).getEnvironments());
                case "SlackSQSMessage" ->
                        logManager.log(LogManager.LogType.SQL, ((SlackSQSMessage) sqsMessage).getLoggableEvent());
                case "KinesisSQSMessage" ->
                        logManager.log(LogManager.LogType.KINESIS, ((KinesisSQSMessage) sqsMessage).getLoggableEvent());
                default -> log.info("Can't Identify Message " + sqsMessage);
            }
        } catch (Exception e) {
            log.error("Error processing events from SQS:" + e.getMessage());
        }

    }
}
