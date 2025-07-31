package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gov.cms.ab2d.optout.OptOutConstants.*;

public class OptOutS3 {
    private final S3Client s3Client;
    private final String fileName;
    private final String bfdBucket;
    private final LambdaLogger logger;

    public OptOutS3(S3Client s3Client, String fileName, String bfdBucket, LambdaLogger logger) {
        this.s3Client = s3Client;
        this.fileName = fileName;
        this.bfdBucket = bfdBucket;
        this.logger = logger;
    }

    public BufferedReader openFileS3() {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bfdBucket)
                    .key(fileName)
                    .build();

            s3Client.headObject(headObjectRequest);

            var getObjectRequest = GetObjectRequest.builder()
                    .bucket(bfdBucket)
                    .key(fileName)
                    .build();

            var s3ObjectResponse = s3Client.getObject(getObjectRequest);
            return new BufferedReader(new InputStreamReader(s3ObjectResponse));
        } catch (SdkClientException ex) {
            var errorMessage = "Unable to load credentials to connect S3 bucket";
            logger.log(errorMessage);
            throw new OptOutException(errorMessage, ex);
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                var errorMessage = "Object " + fileName + " does not exist. " + ex.getMessage();
                logger.log(errorMessage);
                throw new OptOutException(errorMessage, ex);
            } else {
                logger.log(ex.getMessage());
                throw ex;
            }
        }
    }

    public String createResponseOptOutFile(String responseContent) {
        try {
            var key = getOutFileName();

            var objectRequest = PutObjectRequest.builder()
                    .bucket(bfdBucket)
                    .key(key)
                    .build();

            s3Client.putObject(objectRequest, RequestBody.fromString(responseContent));
            return key;
        } catch (AmazonS3Exception ex) {
            var errorMessage = "Response OptOut file cannot be created. ";
            logger.log(errorMessage + ex.getMessage());
            throw new OptOutException(errorMessage, ex);
        }
    }

    public void deleteFileFromS3() {
        try {
            var request = DeleteObjectRequest.builder()
                    .bucket(bfdBucket)
                    .key(fileName)
                    .build();

            s3Client.deleteObject(request);
        } catch (SdkClientException ex) {
            logger.log(ex.getMessage());
        }
    }

    public String getOutFileName() {
        //bfdeft01/ab2d/in/testing.txt
        var prefix = (bfdBucket.contains("prod")) ? "P" : "T";
        var name = prefix + CONF_FILE_NAME
                + new SimpleDateFormat(CONF_FILE_NAME_PATTERN).format(new Date());

        String[] path = fileName.split("in");

        return path[0] + "out/" + name;
    }


}