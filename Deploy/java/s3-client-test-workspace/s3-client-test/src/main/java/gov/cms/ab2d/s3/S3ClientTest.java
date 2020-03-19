package gov.cms.ab2d.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;

class S3ClientTest {
	private static final Logger log = LoggerFactory.getLogger(S3ClientTest.class);

	public static void main(String args[]) {
		// Set region, bucket & filename
		final Region region = Region.of("us-east-1");
		String s3Bucket = "ab2d-optout-data-dev";
		String s3Filename = "T#EFT.ON.ACO.NGD1800.DPRF.D191029.T1135430";

		// Get s3 client for EC2 instance (if applicable)
		S3Client s3Client = getS3ClientForEc2Instance();
		if (s3Client == null) {
			// Get s3 client for development
			s3Client = getS3ClientForDevelopment(region);
		}

		// build GetObjectRequest
		final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(s3Bucket)
				.key(s3Filename)
				.build();

		try (final ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest)) {
		} catch (IOException e) {
			log.error("Unable to get s3 object on AWS  : ", e);
		} catch (SdkServiceException e) {
			log.error("Server error upon calling AWS  : ", e);
			throw e;
		} catch (SdkClientException e) {
			log.error("Client exception on attempting to call AWS : ", e);
			throw e;
		}
	}

	private static S3Client getS3Client(AwsCredentialsProvider provider) {
		S3Client s3Client = null;

		// Try provider used by a container running on an EC2 instance.
		try {
			s3Client = S3Client.builder()
					.credentialsProvider(provider)
					.build();
			s3Client.listBuckets();
			return s3Client;
		} catch (Exception e) {
			log.info("{} not used; {}", provider.getClass(), e.getMessage());
			return null;
		}
	}

	private static S3Client getS3ClientForEc2Instance() {
		return getS3Client(InstanceProfileCredentialsProvider.create());
	}

	private static S3Client getS3ClientForDevelopment(Region region) {
		// Try provider used when an aws credentials file exists and has a default entry
		S3Client s3Client = getS3Client(ProfileCredentialsProvider.create());
		if (s3Client != null) {
			return s3Client;
		}

		// Try provider used when AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment
		// variables are present
		s3Client = getS3Client(EnvironmentVariableCredentialsProvider.create());
		if (s3Client != null) {
			return s3Client;
		}

		// Try provider used when aws.accessKeyId and aws.secretKey system properties are set
		s3Client = getS3Client(SystemPropertyCredentialsProvider.create());
		if (s3Client != null) {
			return s3Client;
		}

		// Try all other providers
		return S3Client.builder()
				.region(region)
				.build();
	}
}
