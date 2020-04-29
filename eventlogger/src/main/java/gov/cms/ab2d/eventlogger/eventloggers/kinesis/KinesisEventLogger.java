package gov.cms.ab2d.eventlogger.eventloggers.kinesis;

import com.amazonaws.ResponseMetadata;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult;
import com.amazonaws.services.kinesisfirehose.model.Record;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import gov.cms.ab2d.eventlogger.EventLogger;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;

@Service
@Slf4j
public class KinesisEventLogger implements EventLogger {
    @Value("${execution.env:dev}")
    private String appEnv;

    @Autowired
    private AmazonKinesisFirehose client;

    @Value("${eventlogger.kinesis.stream.prefix:bfd-insights-ab2d-}")
    private String streamId;

    @Override
    public void log(LoggableEvent event) {
        String json = null;
        event.setEnvironment(appEnv);
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
    }

    static String getJsonString(LoggableEvent event) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
        return mapper.writeValueAsString(event);
    }
}
