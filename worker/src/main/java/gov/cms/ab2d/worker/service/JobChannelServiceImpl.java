package gov.cms.ab2d.worker.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.worker.dto.JobUpdate;
import gov.cms.ab2d.worker.processor.JobMeasure;
import gov.cms.ab2d.worker.processor.JobProgressUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static gov.cms.ab2d.common.service.FeatureEngagement.IN_GEAR;

@Slf4j
@Service
public class JobChannelServiceImpl implements JobChannelService {

    private final AmazonSQS amazonSQS;
    private final ObjectMapper mapper;
    private final JobProgressUpdateService jobProgressUpdateService;
    private final PropertiesService propertiesService;


    @Autowired
    public JobChannelServiceImpl(final AmazonSQS amazonSQS, final ObjectMapper mapper, JobProgressUpdateService jobProgressUpdateService, PropertiesService propertiesService) {
        this.amazonSQS = amazonSQS;
        this.mapper = mapper;
        this.jobProgressUpdateService = jobProgressUpdateService;
        this.propertiesService = propertiesService;
    }

    @Override
    public void sendUpdate(String jobUuid, JobMeasure measure, long value) {
        if (FeatureEngagement.fromString(propertiesService.getPropertiesByKey(Constants.SQS_JOB_UPDATE_ENGAGEMENT).getValue()) == (IN_GEAR)) {
            updateJobThroughQueue(jobUuid, measure, value);
        } else {
            updateJobDirectly(jobUuid, measure, value);
        }
    }

    private void updateJobThroughQueue(String jobUuid, JobMeasure measure, long value) {
        log.info("Sending message to SQS from JobChannelService");

        String queueUrl = amazonSQS.getQueueUrl("ab2d-job-tracking").getQueueUrl();

        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(buildPayload(jobUuid, measure, value));
        amazonSQS.sendMessage(sendMessageRequest);
        log.info("JobChannelService sendUpdate is done");
    }

    private void updateJobDirectly(String jobUuid, JobMeasure measure, long value) {
        jobProgressUpdateService.addMeasure(jobUuid, measure, value);
    }

    private String buildPayload(String jobUuid, JobMeasure measure, long value) {
        try {
            return mapper.writeValueAsString(JobUpdate.builder()
                    .jobUUID(jobUuid)
                    .measure(measure.toString())
                    .value(value)
                    .build());
        } catch (JsonProcessingException e) {
            throw new RuntimeJsonMappingException(
                    String.format("Converting JobUpdate to json failed. %s", e.getCause()));
        }
    }
}
