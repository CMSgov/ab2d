package gov.cms.ab2d.eventlogger.eventloggers.kinesis;

import com.amazonaws.ResponseMetadata;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult;
import com.amazonaws.services.kinesisfirehose.model.Record;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Slf4j
public class KinesisEventProcessor implements Callable<Void> {
    private AmazonKinesisFirehose client;
    private String streamId;
    private LoggableEvent event;

    public KinesisEventProcessor(LoggableEvent event, AmazonKinesisFirehose client, String streamPrefix) {
        this.client = client;
        this.streamId = streamPrefix;
        this.event = event;
    }

    /**
     * Convert an event to a JSON string doing the following:
     *     1. Convert camelcase attribute names to underscores. For example thisFieldName = this_field_name
     *     2. Convert any date/time to UTC
     *
     * @param event - the event we are serializing
     * @return the JSON string
     */
    public static String getJsonString(LoggableEvent event) {
        // Get all the methods, including ones from LoggableEvent
        Method[] methods = event.getClass().getDeclaredMethods();
        Method[] superMethods = event.getClass().getSuperclass().getDeclaredMethods();
        List<Method> methodList = List.of(methods);
        List<Method> superMethodList = List.of(superMethods);
        List<Method> allMethods = new ArrayList<>(methodList);
        allMethods.addAll(superMethodList);

        // Retrieve any get methods, these are our public attributes
        List<Method> getMethods = allMethods.stream().filter(m -> m.getName().startsWith("get")).collect(Collectors.toList());

        // From these methods, create a hash map for fields and their values
        Map<String, Object> vals = new HashMap<>();
        for (Method m : getMethods) {
            try {
                // Retrieve the value of the field
                Object attValue = m.invoke(event);
                // If we are an OffsetDateTime, convert to UTC, then make it a string in the correct format
                if (attValue != null && attValue.getClass() == OffsetDateTime.class) {
                    OffsetDateTime timeValue = (OffsetDateTime) attValue;
                    attValue = timeValue.atZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME);
                }
                // Convert the method name to the camelcase name and remove 'get'. For example, getJobId = job_id
                String newAttName = camelCaseToUnderscore(m.getName().replace("get", ""));
                vals.put(newAttName, attValue);
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.info("Unable to call " + m.getName() + " on " + event.getClass().getSimpleName());
            }
        }
        // Create a JSON object from the map
        JSONObject jsonObject = new JSONObject(vals);
        // Convert to string
        return jsonObject.toString();
    }

    @Override
    public Void call() {
        String json = null;
        try {
            json = getJsonString(event) + "\n";
            Record record = new Record()
                    .withData(ByteBuffer.wrap(json.getBytes()));

            PutRecordRequest putRecordRequest = new PutRecordRequest();
            String className = camelCaseToUnderscore(event.getClass().getSimpleName());
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

    static String camelCaseToUnderscore(String val) {
        return val.replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();
    }
}
