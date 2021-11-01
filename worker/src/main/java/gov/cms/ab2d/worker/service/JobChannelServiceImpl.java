package gov.cms.ab2d.worker.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import gov.cms.ab2d.worker.processor.JobMeasure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobChannelServiceImpl implements JobChannelService {

    @Override
    public void sendUpdate(String jobUuid, JobMeasure measure, long value) {
        final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
        String queueUrl = sqs.getQueueUrl("ab2d-job-tracking").getQueueUrl();

        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody("hello world")
                .withDelaySeconds(1);
        sqs.sendMessage(sendMessageRequest);
    }
}
