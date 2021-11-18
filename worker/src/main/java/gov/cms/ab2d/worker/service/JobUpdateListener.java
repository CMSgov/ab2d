package gov.cms.ab2d.worker.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import gov.cms.ab2d.worker.processor.JobMeasure;
import gov.cms.ab2d.worker.processor.JobProgressUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.Acknowledgment;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobUpdateListener {

    private final JobProgressUpdateService jobProgressUpdateService;

    public JobUpdateListener(JobProgressUpdateService jobProgressUpdateService) {
        this.jobProgressUpdateService = jobProgressUpdateService;
    }

    // TODO - Add a mapper so that this method can be directly invoked with a JSONObject.
    @SqsListener(value = "ab2d-job-tracking", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void processJobProgressUpdate(String payload, Acknowledgment ack) {
        log.info("JobUpdateListener: Processing message from SQS: " + payload);
        JsonObject jobUdate = new Gson().fromJson(payload, JsonObject.class);
        log.info("JobUpdateListener: Done parsing: " + jobUdate.size());
        boolean consumed = jobProgressUpdateService.addMeasure(jobUdate.get("job_uuid").getAsString(),
                JobMeasure.valueOf(jobUdate.get("measure").getAsString()),
                jobUdate.get("value").getAsLong());

        if (consumed) {
            ack.acknowledge();
            log.info("JobUpdateListener: acknowledged the message");
        }
        log.info("JobUpdateListener: all done");
    }
}
