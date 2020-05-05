package gov.cms.ab2d.eventlogger.eventloggers.kinesis;

import com.amazonaws.ResponseMetadata;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult;
import com.amazonaws.services.kinesisfirehose.model.Record;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

@Slf4j
public class KinesisEventProcessor implements Callable<Void> {
    private AmazonKinesisFirehose client;
    private String streamId;
    private LoggableEvent event;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    public KinesisEventProcessor(LoggableEvent event, AmazonKinesisFirehose client, String streamPrefix) {
        this.client = client;
        this.streamId = streamPrefix;
        this.event = event;
    }

    static String getJsonString(LoggableEvent event) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(event);
    }

    @Override
    public Void call() {
        String json = null;
        try {
            json = getJsonString(event) + "\n";
            Record record = new Record()
                    .withData(ByteBuffer.wrap(json.getBytes()));

            PutRecordRequest putRecordRequest = new PutRecordRequest();
            String className = event.getClass().getSimpleName().toLowerCase();
            putRecordRequest.setDeliveryStreamName(streamId + className);
            putRecordRequest.setRecord(record);

            PutRecordResult putRecordResult = client.putRecord(putRecordRequest);
            ResponseMetadata data = putRecordResult.getSdkResponseMetadata();
            event.setAwsId(data.getRequestId());
        } catch (Exception ex) {
            log.error("Error in pushing event to Kinesis " + json + " - " + ex.getMessage());
        }
        return null;
    }
}
