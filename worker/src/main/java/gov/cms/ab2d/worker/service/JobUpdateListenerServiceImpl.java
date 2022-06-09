package gov.cms.ab2d.worker.service;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.util.Topics;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.worker.dto.JobUpdate;
import gov.cms.ab2d.worker.dto.SNSMessage;
import gov.cms.ab2d.worker.processor.JobMeasure;
import gov.cms.ab2d.worker.processor.JobProgressUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.security.SecureRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static gov.cms.ab2d.common.util.Constants.BASE_SQS_QUEUE;
import static gov.cms.ab2d.common.util.Constants.SNS_QUEUE;

@Slf4j
@Service
public class JobUpdateListenerServiceImpl implements SqsService {

    private final int randomCharCount = 5;
    private final AmazonSQS amazonSqs;

    private final AmazonSNS amazonSNS;

    private final ObjectMapper mapper;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private String queueName;

    private String queueUrl;

    private final JobProgressUpdateService jobProgressUpdateService;

    public JobUpdateListenerServiceImpl(AmazonSQS amazonSqs, AmazonSNS amazonSNS, ObjectMapper mapper, JobProgressUpdateService jobProgressUpdateService) {
        this.amazonSqs = amazonSqs;
        this.amazonSNS = amazonSNS;
        this.mapper = mapper;
        this.jobProgressUpdateService = jobProgressUpdateService;
    }

    @PostConstruct
    private void initiate() {
        String randomChars = RandomStringUtils.random(randomCharCount, 0, 0, true, true, null, new SecureRandom());
        queueName = BASE_SQS_QUEUE + "-" + randomChars;
        queueUrl = amazonSqs.createQueue(queueName).getQueueUrl();
        log.info("Queue {} has been created", queueName);
        Topics.subscribeQueue(amazonSNS, amazonSqs,
                amazonSNS.createTopic(SNS_QUEUE).getTopicArn(), queueUrl);
        log.info("Queue {} has subscribed to {} SNS queue", queueName, SNS_QUEUE);
        startSqsListenerThread();
    }

    @PreDestroy
    private void cleanup() {
        amazonSqs.deleteQueue(queueName);
        log.info("Queue {} has been deleted", queueName);
    }

    private void startSqsListenerThread() {
        executor.scheduleWithFixedDelay(this::pollJobUpdateSqs, 0, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void pollJobUpdateSqs() {
        ReceiveMessageRequest request = new ReceiveMessageRequest(queueUrl);
        request.setWaitTimeSeconds(5);
        amazonSqs.receiveMessage(new ReceiveMessageRequest(queueUrl))
                .getMessages()
                .forEach(message -> {
                    try {
                        SNSMessage snsMessage = mapper.readValue(message.getBody(), SNSMessage.class);
                        JobUpdate update = mapper.readValue(snsMessage.getMessage(), JobUpdate.class);
                        processMessage(snsMessage, update);
                    } catch (Exception e) {
                        log.info("Message sent to " + queueName + " failed to deserialize. {}", message, e);
                    } finally {
                        amazonSqs.deleteMessage(queueUrl, message.getReceiptHandle());
                    }
                });
    }

    public void processMessage(SNSMessage snsMessage, JobUpdate update) {
        if (jobProgressUpdateService.hasJob(snsMessage.getSubject())) {
            jobProgressUpdateService.addMeasure(snsMessage.getSubject(), JobMeasure.valueOf(update.getMeasure()), update.getValue());
            log.info("Measure updated");
        }

    }

}