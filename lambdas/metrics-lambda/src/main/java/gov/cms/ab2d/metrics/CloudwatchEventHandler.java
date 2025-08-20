package gov.cms.ab2d.metrics;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.ab2d.eventclient.events.MetricsEvent;
import gov.cms.ab2d.eventclient.messages.GeneralSQSMessage;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static gov.cms.ab2d.eventclient.events.MetricsEvent.State.END;
import static gov.cms.ab2d.eventclient.events.MetricsEvent.State.START;


// Catches cloudwatch alerts, extracts what we care about, then send an event to the ab2d-event sqs queue
public class CloudwatchEventHandler implements RequestHandler<SNSEvent, String> {
    private static AmazonSQS amazonSQS;

    private final String environment = Optional.ofNullable(System.getenv("environment"))
            .orElse("local") + "-";

    private final String sqsQueueUrl = Optional.ofNullable(System.getenv("AWS_SQS_EVENTS_URL"))
            .orElse("local") + "-";


    private final String queueName = deriveSqsQueueName(sqsQueueUrl);

    // AWS sends an object that's not wrapped with type info. The event service expects the wrapper.
    // Since there's not an easy way to enable/disable type wrapper just have 2 mappers.
    private final ObjectMapper inputMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JodaModule())
            .registerModule(new JavaTimeModule());

    private final ObjectMapper outputMapper = new ObjectMapper()
            .activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY)
            .registerModule(new JodaModule())
            .registerModule(new JavaTimeModule());

    static {
        amazonSQS = setup();
    }

    public static String deriveSqsQueueName(String url) {
        try {
            String[] tokens = url.split("/");
            return tokens[tokens.length-1];
        }
        catch (Exception e) {
            throw new MetricsLambdaException("Unable to derive SQS queue name from URL: " + url);
        }
    }


    private static AmazonSQS setup() {
        if (!StringUtils.isNullOrEmpty(System.getenv("IS_LOCALSTACK"))) {
            System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");
            return AmazonSQSAsyncClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder
                            .EndpointConfiguration("https://localhost:4566",
                            Regions.US_EAST_1.getName()))
                    .build();
        } else {
            return AmazonSQSAsyncClientBuilder.standard()
                    .build();
        }
    }

    @Override
    public String handleRequest(SNSEvent snsEvent, Context context) {
        final LambdaLogger log = context.getLogger();
        snsEvent.getRecords()
                .forEach(snsRecord -> sendMetric(snsRecord, log));
        return "OK";
    }

    private void sendMetric(SNSEvent.SNSRecord snsRecord, LambdaLogger log) {
        final SendMessageRequest request = new SendMessageRequest();
        final String queue = this.queueName;
        final String service;
        try {
            final MetricAlarm alarm = inputMapper.readValue(Optional.ofNullable(snsRecord.getSNS())
                    .orElse(new SNSEvent.SNS())
                    .getMessage(), MetricAlarm.class);
            OffsetDateTime time = alarm.getStateChangeTime() != null
                    ? OffsetDateTime.parse(fixDate(alarm.getStateChangeTime()))
                    : OffsetDateTime.now();
            request.setQueueUrl(amazonSQS.getQueueUrl(queue)
                    .getQueueUrl());
            service = Optional.ofNullable(alarm.getAlarmName())
                    .orElseThrow(() -> new EventDataException("AlarmName is null"))
                    .replace(environment, "");
            request.setMessageBody(outputMapper.writeValueAsString(new GeneralSQSMessage(MetricsEvent.builder()
                    .service(service)
                    .eventDescription(alarm.getAlarmDescription())
                    .timeOfEvent(time)
                    //This might need more work later if AWS is sending unknown states regularly
                    .stateType(from(alarm.getNewStateValue()))
                    .build())));
        } catch (Exception e) {
            log.log(String.format("Handling lambda failed %s", exceptionToString(e)));
            return;
        }
        log.log(String.format("Sending %s to %s", service, queue));
        amazonSQS.sendMessage(request);
    }

    private String exceptionToString(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }


    private String fixDate(String date) {
        StringBuilder builder = new StringBuilder(date);
        return builder.insert(builder.length() - 2, ":")
                .toString();
    }


    private MetricsEvent.State from(String stateValue) {
        return Stream.of(stateValue)
                .filter(value1 -> List.of("OK", "ALARM")
                        .contains(value1))
                .map(state -> stateValue.equals("OK") ? END : START)
                .findFirst()
                .orElseThrow(() -> new EventDataException(String.format("AWS provided Unknown State %s", stateValue)));
    }
}
