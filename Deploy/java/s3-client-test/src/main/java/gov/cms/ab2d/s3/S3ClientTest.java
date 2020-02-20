package gov.cms.ab2d.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.InputStreamReader;

class S3ClientTest
{
    private static final Logger log = LoggerFactory.getLogger(S3ClientTest.class);

    public static void main(String args[]) 
    {
	// Set S3 region
	String s3Region = "us-east-1";
        final Region region = Region.of(s3Region);

	// Set S3 bucket
	String s3Bucket = "ab2d-optout-data-dev";

	// Set S3 file name
	String s3Filename = "T#EFT.ON.ACO.NGD1800.DPRF.D191029.T1135430";

        // build S3 client
	// - note that the EnvironmentVariableCredentialsProvider expects
	//   AWS_REGION, AWS_ACCESS_KEY_ID, and AWS_SECRET_ACCESS_KEY environment variables;
	//   these variables are associated with the 'ab2d-s3-signing' IAM user that is maintained
	//   within the AB2D managament AWS account.
	final S3Client s3Client = S3Client.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

	// build GetObjectRequest
        final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Bucket)
                .key(s3Filename)
                .build();

	try {
            final ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);
            InputStreamReader inputStreamReader = new InputStreamReader(responseInputStream);
        } catch (SdkServiceException e) {
            log.error("Server error upon calling AWS  : ", e);
            throw e;
        } catch (SdkClientException e) {
            log.error("Client exception on attempting to call AWS : ", e);
            throw e;
        }
	
    } 
}
