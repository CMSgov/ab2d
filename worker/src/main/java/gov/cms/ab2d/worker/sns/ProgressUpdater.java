package gov.cms.ab2d.worker.sns;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.worker.dto.JobUpdate;
import gov.cms.ab2d.worker.processor.JobMeasure;
import gov.cms.ab2d.worker.processor.JobProgressService;
import gov.cms.ab2d.worker.processor.JobProgressUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.config.annotation.NotificationMessage;
import org.springframework.cloud.aws.messaging.config.annotation.NotificationSubject;
import org.springframework.cloud.aws.messaging.endpoint.NotificationStatus;
import org.springframework.cloud.aws.messaging.endpoint.annotation.NotificationMessageMapping;
import org.springframework.cloud.aws.messaging.endpoint.annotation.NotificationSubscriptionMapping;
import org.springframework.cloud.aws.messaging.endpoint.annotation.NotificationUnsubscribeConfirmationMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Controller
@RequestMapping("/ab2d-job-tracking")
public class ProgressUpdater {

    @Value("${ab2d.sns.base.address:http://host.docker.internal:8080}")
    private String snsBaseAddress;
    private final JobProgressUpdateService jobProgressUpdateService;

    private final AmazonSNS amazonSns;
    private final ObjectMapper mapper;

    public ProgressUpdater(JobProgressUpdateService jobProgressUpdateService, AmazonSNS amazonSns, ObjectMapper mapper) {
        this.jobProgressUpdateService = jobProgressUpdateService;
        this.amazonSns = amazonSns;
        this.mapper = mapper;
    }

    @PostConstruct
    public void setup() {
        String arn = amazonSns.createTopic("ab2d-job-tracking").getTopicArn();
        SubscribeResult subscribeResult = amazonSns.subscribe(arn, "http", snsBaseAddress + "/ab2d-job-tracking");
        log.info(subscribeResult.getSubscriptionArn());
    }

    @PreDestroy
    public void destroy() {
        amazonSns.unsubscribe(snsBaseAddress + "/ab2d-job-tracking");
        System.out.println("Shutdown - Unsubscribed from ab2d-job-tracking");
    }

    @NotificationUnsubscribeConfirmationMapping
    public void confirmUnsubscribeMessage(
            NotificationStatus notificationStatus) {
        notificationStatus.confirmSubscription();
        log.info("Unsubscribed: " + notificationStatus);
    }

    @NotificationMessageMapping
    public void receiveNotification(@NotificationMessage String message,
                                    @NotificationSubject String subject) throws JsonProcessingException {
        if (jobProgressUpdateService.hasJob(subject)) { //Ignore untracked job messages
            JobUpdate jobUpdate = mapper.readValue(message, JobUpdate.class);
            log.info("JobUpdateListener: Processing message from SNS: " + jobUpdate);
            log.info("JobUpdateListener: Done parsing: " + jobUpdate);
            jobProgressUpdateService.addMeasure(subject,
                    JobMeasure.valueOf(jobUpdate.getMeasure()),
                    jobUpdate.getValue());
            log.info("JobUpdateListener: all done");
        }
    }

    @NotificationSubscriptionMapping
    public void confirmSubscriptionMessage(
            NotificationStatus notificationStatus) {
        notificationStatus.confirmSubscription();
        log.info("Subscribed: " + notificationStatus);
    }
}