package gov.cms.ab2d.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.io.InputStreamReader;

class S3ClientTest
{
	private static final Logger log = LoggerFactory.getLogger(S3ClientTest.class);
	
	public static void main(String args[]) 
	{
		// Set region
		String s3Region = "us-east-1";
		final Region region = Region.of(s3Region);
		
		// Set S3 bucket
		String s3Bucket = "ab2d-optout-data-dev";
		
		// Set S3 file name
		String s3Filename = "T#EFT.ON.ACO.NGD1800.DPRF.D191029.T1135430";
		
		// Get s3 client for EC2 instance (if applicable)
		S3Client s3Client = getS3ClientForEc2Instance(region, s3Bucket);
		if (s3Client == null) {
			// Get s3 client for development
			s3Client = getS3ClientForDevelopment(region, s3Bucket);
		}
		
		// build GetObjectRequest
		final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(s3Bucket)
				.key(s3Filename)
				.build();
		
		try {
			final ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);
			InputStreamReader inputStreamReader = new InputStreamReader(responseInputStream);
			try {
				inputStreamReader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (SdkServiceException e) {
			log.error("Server error upon calling AWS  : ", e);
			throw e;
		} catch (SdkClientException e) {
			log.error("Client exception on attempting to call AWS : ", e);
			throw e;
		}
	}
	
	private static S3Client getS3ClientForEc2Instance(Region region, String s3Bucket) {
		S3Client s3Client = null;
		
		// Provider used by a container running on an EC2 instance.
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
		
		// Provider used when an aws credentials file exists and has a default entry.
		try {
			s3Client = S3Client.builder()
					.credentialsProvider(ProfileCredentialsProvider.create())
					.build();
			bucketList = s3Client.listBuckets();
			return s3Client;
		} catch (Exception e) {
			log.info("ProfileCredentialsProvider not used; {}", e.getMessage());
		}
		
		// Provider used when AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables
		// are present.
		try {
			s3Client = S3Client.builder()
					.credentialsProvider(EnvironmentVariableCredentialsProvider.create())
					.build();
			bucketList = s3Client.listBuckets();
			return s3Client;
		} catch (Exception e) {
			log.info("EnvironmentVariableCredentialsProvider not used; {}", e.getMessage());
		}
		
		// Provider used when aws.accessKeyId and aws.secretKey system properties are set.
		try {
			s3Client = S3Client.builder()
					.credentialsProvider(SystemPropertyCredentialsProvider.create())
					.build();
			bucketList = s3Client.listBuckets();
			return s3Client;
		} catch (Exception e) {
			log.info("SystemPropertyCredentialsProvider not used; {}", e.getMessage());
		}
		
		// Last chance attempt using all other providers.
		if (bucketList == null) {
			s3Client =  S3Client.builder()
					.region(region)
					.build();
			}
		return s3Client;
		
	}

}
