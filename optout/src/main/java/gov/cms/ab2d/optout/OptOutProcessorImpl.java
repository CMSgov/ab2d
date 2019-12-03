package gov.cms.ab2d.optout;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
@Component
public class OptOutProcessorImpl implements OptOutProcessor {

    @Value("${s3.region}")
    private String s3Region;

    @Value("${s3.bucket}")
    private String s3Bucket;

    @Value("${s3.filename}")
    private String s3Filename;

    @Override
    @Transactional
    public void process() {

        //set region
        final Region region = Region.of(s3Region);


        // build S3 client
        final S3Client s3Client =  S3Client.builder().region(region).build();


        // build GetObjectRequest
        final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Bucket)
                .key(s3Filename)
                .build();



        long linesReadCount = 0;
        try {
            var responseInputStream = s3Client.getObject(getObjectRequest);

            var bufferedReader = new BufferedReader(new InputStreamReader(responseInputStream));
            linesReadCount = bufferedReader.lines().count();

        } catch (SdkServiceException e) {
            log.error("Server error upon calling AWS  : ", e);
            throw e;
        } catch (SdkClientException e) {
            log.error("Client exception on attempting to call AWS : ", e);
            throw e;
        }

        log.info("Read [{}] lines from S3 file", linesReadCount);
    }


}
