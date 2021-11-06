package gov.cms.ab2d.worker.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.gson.JsonObject;
import gov.cms.ab2d.worker.processor.JobMeasure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobChannelServiceImpl implements JobChannelService {

    @Override
    public void sendUpdate(String jobUuid, JobMeasure measure, long value) {
        log.info("Sending message to SQS from JobChannelService");

        final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
        String queueUrl = sqs.getQueueUrl("ab2d-job-tracking").getQueueUrl();

        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(buildPayload(jobUuid, measure, value));
        sqs.sendMessage(sendMessageRequest);
        log.info("JobChannelService sendUpdate is done");
    }

    private String buildPayload(String jobUuid, JobMeasure measure, long value) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("job_uuid", jobUuid);
        jsonObject.addProperty("measure", measure.toString());
        jsonObject.addProperty("value", value);
        return jsonObject.toString();
    }

    //     private final JobProgressUpdateService jobProgressUpdateService;
//    @ServiceActivator(inputChannel = "jobProgress")
//    @SqsListener(value = "sample3", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
//    public void progressReceiver(JsonObject payload, Acknowledgment ack) {
//
//    }
}
