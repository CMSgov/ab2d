package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.event.S3EventNotification;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import static gov.cms.ab2d.optout.OptOutConstants.ENDPOINT;

public class OptOutHandler implements RequestHandler<SQSEvent, Void> {

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        for (SQSEvent.SQSMessage msg : sqsEvent.getRecords()) {
            processSQSMessage(msg, context);
        }
        return null;
    }

    public void processSQSMessage(SQSEvent.SQSMessage msg, Context context) {
        var logger = context.getLogger();
        try {
            logger.log("OptOut Lambda started. Processing message from SQS " + msg.getBody());

            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(msg.getBody());
            var s3EventMessage = json.get("Message");
            var notification = S3EventNotification.parseJson(s3EventMessage.toString()).getRecords().get(0);

            var optOutProcessing = processorInit(logger);
            var optOutResults = optOutProcessing.process(getFileName(notification), getBucketName(notification), ENDPOINT);
            logResults(optOutResults, logger);
        } catch (Exception ex) {
            logger.log("An error occurred");
            throw new OptOutException("An error occurred", ex);
        }
    }

    public void logResults(OptOutResults optOutResults, LambdaLogger logger) {
        if (optOutResults != null) {
            logger.log("OptOut Lambda completed. Total records processed today is: totaltoday=" + optOutResults.getTotalToday()
                + " Number of opt in today is: todayin=" + optOutResults.getOptInToday()
                + " Number of opt out today is: todayout=" + optOutResults.getOptOutToday()
                + " Total records processed to date is: totaltodate=" + optOutResults.getTotalAllTime()
                + " Total number of opt in is: totalin=" + optOutResults.getOptInTotal()
                + " Total number of opt out is: totalout=" + optOutResults.getOptOutTotal()
            );
        }
    }

    public OptOutProcessor processorInit(LambdaLogger logger) {
        return new OptOutProcessor(logger);
    }

    public String getBucketName(S3EventNotification.S3EventNotificationRecord notificationRecord) {
        return notificationRecord.getS3().getBucket().getName();
    }

    public String getFileName(S3EventNotification.S3EventNotificationRecord notificationRecord) {
        return notificationRecord.getS3().getObject().getUrlDecodedKey();
    }


}
