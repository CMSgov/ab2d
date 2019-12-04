package gov.cms.ab2d.optout;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.InputStreamReader;

@Slf4j
@Component
public class S3GatewayImpl implements S3Gateway {

    //    private static final Pattern FILENAME_PATTERN = Pattern.compile("(P|T)#EFT\\.ON\\.ACO\\.NGD1800\\.DPRF\\.D\\d{6}\\.T\\d{7}");

    @Value("${s3.region}")
    private String s3Region;

    @Value("${s3.bucket}")
    private String s3Bucket;

    @Value("${s3.filename}")
    private String s3Filename;

    @Override
    public InputStreamReader getS3Object() {
        //set region
        final Region region = Region.of(s3Region);


        // build S3 client
        final S3Client s3Client =  S3Client.builder().region(region).build();


        // build GetObjectRequest
        final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Bucket)
                .key(s3Filename)
                .build();

        try {
            final ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);
            return new InputStreamReader(responseInputStream);
        } catch (SdkServiceException e) {
            log.error("Server error upon calling AWS  : ", e);
            throw e;
        } catch (SdkClientException e) {
            log.error("Client exception on attempting to call AWS : ", e);
            throw e;
        }

    }
}
