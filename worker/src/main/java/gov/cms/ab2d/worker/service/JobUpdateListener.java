package gov.cms.ab2d.worker.service;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.Acknowledgment;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobUpdateListener {

    @SqsListener(value = "ab2d-job-tracking", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void processJobProgressUpdate(JsonObject payload, Acknowledgment ack) {
//        LOGGER.info("Incoming S3EventNoticiation: " + event.toJson());
        log.info("Processing message from SQS: " + payload.toString());
        ack.acknowledge();
    }
}
