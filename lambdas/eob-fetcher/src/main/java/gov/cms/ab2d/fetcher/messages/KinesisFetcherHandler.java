package gov.cms.ab2d.fetcher.messages;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent.KinesisEventRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.ab2d.fetcher.model.JobFetchPayload;
import gov.cms.ab2d.worker.processor.PatientClaimsProcessorImpl;
import gov.cms.ab2d.worker.processor.ProgressTrackerUpdate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;

/**
 * The entry point invoked by AWS
 */
@SuppressWarnings("unused")
public class KinesisFetcherHandler implements RequestHandler<KinesisEvent, String> {
    private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    private static final AnnotationConfigApplicationContext CONTEXT = new AnnotationConfigApplicationContext(
            "gov.cms.ab2d.fetcher", "gov.cms.ab2d.worker", "gov.cms.ab2d.bfd");

    private final PatientClaimsProcessorImpl patientClaimsProcessor;

    public KinesisFetcherHandler(PatientClaimsProcessorImpl patientClaimsProcessor) {
        this.patientClaimsProcessor = patientClaimsProcessor;
        CONTEXT.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @Override
    public String handleRequest(KinesisEvent event, Context context) {
        LambdaLogger logger = context.getLogger();

        logger.log("Kinesis event version.");
        String response = "200 OK";

        ProgressTrackerUpdate update = new ProgressTrackerUpdate();
        for (KinesisEventRecord eventRecord : event.getRecords()) {
            try {
                JobFetchPayload jobFetchPayload =
                        objectMapper.readValue(eventRecord.getKinesis().getData().array(), JobFetchPayload.class);
                patientClaimsProcessor.processBeneficiaries(jobFetchPayload, update);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        // TODO - push ProgressTrackerUpdate updates

        return response;
    }
}

