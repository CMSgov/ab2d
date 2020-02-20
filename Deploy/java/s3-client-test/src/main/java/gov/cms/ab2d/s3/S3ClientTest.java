package gov.cms.ab2d.s3;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.InputStreamReader;

@Slf4j
class S3ClientTest
{ 
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
        final S3Client s3Client =  S3Client.builder().region(region).build();

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
