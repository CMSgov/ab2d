package gov.cms.ab2d.worker.service;

import com.amazonaws.services.sns.AmazonSNS;
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
import static gov.cms.ab2d.common.util.Constants.SNS_QUEUE;

@Slf4j
@Service
public class JobChannelServiceImpl implements JobChannelService {

    private final AmazonSNS amazonSNS;
    private final ObjectMapper mapper;
    private final JobProgressUpdateService jobProgressUpdateService;
    private final PropertiesService propertiesService;


    @Autowired
    public JobChannelServiceImpl(final AmazonSNS amazonSNS, final ObjectMapper mapper, JobProgressUpdateService jobProgressUpdateService, PropertiesService propertiesService) {
        this.amazonSNS = amazonSNS;
        this.mapper = mapper;
        this.jobProgressUpdateService = jobProgressUpdateService;
        this.propertiesService = propertiesService;
    }

    @Override
    public void sendUpdate(String jobUuid, JobMeasure measure, long value) {
        if (FeatureEngagement.fromString(propertiesService.getPropertiesByKey(Constants.SNS_JOB_UPDATE_ENGAGEMENT).getValue()) == (IN_GEAR)) {
            updateJobThroughSns(jobUuid, measure, value);
        } else {
            updateJobDirectly(jobUuid, measure, value);
        }
    }



    private void updateJobThroughSns(String jobUuid, JobMeasure measure, long value) {
        log.info("Sending message {} to SNS from JobChannelService", jobUuid);
        String arn = amazonSNS.createTopic(SNS_QUEUE).getTopicArn();
        amazonSNS.publish(arn, buildPayload(measure, value), jobUuid);
        log.info("JobChannelService sendUpdate is done");
    }

    private void updateJobDirectly(String jobUuid, JobMeasure measure, long value) {
        log.info("Updating job {} directly", jobUuid);
        jobProgressUpdateService.addMeasure(jobUuid, measure, value);
    }

    private String buildPayload(JobMeasure measure, long value) {
        try {
            return mapper.writeValueAsString(JobUpdate.builder()
                    .measure(measure.toString())
                    .value(value)
                    .build());
        } catch (JsonProcessingException e) {
            throw new RuntimeJsonMappingException(
                    String.format("Converting JobUpdate to json failed. %s", e.getCause()));
        }
    }
}
