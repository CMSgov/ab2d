package gov.cms.ab2d.optout.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

import java.io.InputStreamReader;
import java.util.regex.Pattern;

@Slf4j
@Component
public class S3GatewayImpl implements S3Gateway {

    private static final Pattern FILENAME_PATTERN = Pattern.compile("(P|T)#EFT\\.ON\\.ACO\\.NGD1800\\.DPRF\\.D\\d{6}\\.T\\d{7}");

    @Value("${s3.region}")
    private String s3Region;

    @Value("${s3.bucket}")
    private String s3Bucket;

    @Value("${s3.filename}")
    private String s3Filename;

    @Override
    public InputStreamReader getOptOutFile() {

        validateFileName();

        // Set region
        final Region region = Region.of(s3Region);

        // Get s3 client for EC2 instance (if applicable)
        S3Client s3Client = getS3ClientForEc2Instance(region, s3Bucket);
        if (s3Client == null) {
            // Get s3 client for development
            s3Client = getS3ClientForDevelopment(region, s3Bucket);
        }

        // Build GetObjectRequest
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

    private void validateFileName() {
        if (!FILENAME_PATTERN.matcher(s3Filename).matches()) {
            final String errMsg = "Filename is invalid ";
            log.error("{} : {} ", errMsg, s3Filename);
            throw new RuntimeException(errMsg);
        }
    }

    private static S3Client getS3ClientForEc2Instance(Region region, String s3Bucket) {
        S3Client s3Client = null;

        // Try provider used by a container running on an EC2 instance
        try {
            s3Client = S3Client.builder()
                         .credentialsProvider(InstanceProfileCredentialsProvider.create())
                         .build();
            return s3Client;
        } catch (Exception e) {
            log.info("InstanceProfileCredentialsProvider not used; {}", e.getMessage());
        }
        return s3Client;
    }

    private static S3Client getS3ClientForDevelopment(Region region, String s3Bucket) {
        S3Client s3Client = null;
        ListBucketsResponse bucketList = null;

        // Set the "aws.region" system property
        System.setProperty("aws.region", region.id());

        // Try provider used when an aws credentials file exists and has a default entry
        try {
            s3Client = S3Client.builder()
                            .credentialsProvider(ProfileCredentialsProvider.create())
                            .build();
            bucketList = s3Client.listBuckets();
            return s3Client;
        } catch (Exception e) {
            log.info("ProfileCredentialsProvider not used; {}", e.getMessage());
        }

        // Try provider used when AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment
        // variables are present
        try {
            s3Client = S3Client.builder()
                         .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                         .build();
            bucketList = s3Client.listBuckets();
            return s3Client;
        } catch (Exception e) {
            log.info("EnvironmentVariableCredentialsProvider not used; {}", e.getMessage());
        }

        // Try provider used when aws.accessKeyId and aws.secretKey system properties are set
        try {
            s3Client = S3Client.builder()
                         .credentialsProvider(SystemPropertyCredentialsProvider.create())
                         .build();
            bucketList = s3Client.listBuckets();
            return s3Client;
        } catch (Exception e) {
            log.info("SystemPropertyCredentialsProvider not used; {}", e.getMessage());
        }

        // Try all other providers
        if (bucketList == null) {
            s3Client = S3Client.builder()
                         .region(region)
                         .build();
        }
        return s3Client;
    }
}
