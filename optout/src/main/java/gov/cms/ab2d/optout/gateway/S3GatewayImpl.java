package gov.cms.ab2d.optout.gateway;

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
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class S3GatewayImpl implements S3Gateway {

    private static final Pattern FILENAME_PATTERN = Pattern.compile("(P|T)#EFT\\.ON\\.ACO\\.NGD1800\\.DPRF\\.D\\d{6}\\.T\\d{7}");

    @Value("${s3.region}")
    private String s3Region;

    @Value("${s3.bucket}")
    private String s3Bucket;


    @Override
    public List<String> listOptOutFiles() {

        // set region
        final Region region = Region.of(s3Region);


        // build S3 client
        final S3Client s3Client =  S3Client.builder().region(region).build();

        // create a ListObjectsRequest
        final ListObjectsRequest listObjects = ListObjectsRequest.builder()
                .bucket(s3Bucket)
                .build();

        // get a list of objects in the bucket
        var listObjectsResponse = s3Client.listObjects(listObjects);
        var s3Objects = listObjectsResponse.contents();

        // extract key (filename) from the list of S3Objects
        final List<String> fileNames = s3Objects.stream()
                .map(s3Object -> {
                    log.info("Found [{}] - Size [{}] KB", s3Object.key(), s3Object.size() / 1024);
                    return s3Object.key();
                })
                .collect(Collectors.toList());

        return fileNames;
    }


    @Override
    public InputStreamReader getOptOutFile(String fileName) {

        validateFileName(fileName);

        // set region
        final Region region = Region.of(s3Region);


        // build S3 client
        final S3Client s3Client =  S3Client.builder().region(region).build();


        final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Bucket)
                .key(fileName)
                .build();

        try {
            final ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);
            return new InputStreamReader(responseInputStream, StandardCharsets.UTF_8);
        } catch (SdkServiceException e) {
            log.error("Server error upon calling AWS  : ", e);
            throw e;
        } catch (SdkClientException e) {
            log.error("Client exception on attempting to call AWS : ", e);
            throw e;
        }

    }


    private void validateFileName(String fileName) {
        if (!FILENAME_PATTERN.matcher(fileName).matches()) {
            final String errMsg = "Filename is invalid ";
            log.error("{} : {} ", errMsg, fileName);
            throw new RuntimeException(errMsg);
        }
    }
}
