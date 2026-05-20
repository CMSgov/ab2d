package gov.cms.ab2d.optout;

import io.findify.s3mock.S3Mock;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;

import static gov.cms.ab2d.optout.OptOutConstants.S3_REGION;
import static gov.cms.ab2d.optout.OptOutConstantsTest.*;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class S3MockAPIExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {
    private static final S3Mock API = S3Mock.create(8001, "/tmp/s3");
    private static S3Client s3Client;
    private static boolean started = false;

    public static S3Client getS3Client() {
        return s3Client;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (!started) {
            started = true;
            context.getRoot().getStore(GLOBAL).put("S3MockAPIExtension", this);

            API.start();

            s3Client = S3Client.builder()
                    .region(S3_REGION)
                    .endpointOverride(new URI(TEST_ENDPOINT))
                    .build();

            createBucket();
        }
    }

    @Override
    public void close() {
        deleteBucket();
        s3Client.close();
        API.stop();
    }

    private static void createBucket() {
        var bucketRequest = CreateBucketRequest.builder()
                .bucket(TEST_BFD_BUCKET_NAME)
                .build();

        s3Client.createBucket(bucketRequest);
    }

    public static void createFile(String content, String fileName) {
        var objectRequest = PutObjectRequest.builder()
                .bucket(TEST_BFD_BUCKET_NAME)
                .key(fileName)
                .build();

        s3Client.putObject(objectRequest, RequestBody.fromString(content));
    }

    public static boolean isObjectExists(String fileName) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(TEST_BFD_BUCKET_NAME)
                    .key(fileName)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                return false;
            } else {
                throw ex;
            }
        }
    }

    public static void deleteFile(String fileName) {
        if (isObjectExists(fileName)) {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(TEST_BFD_BUCKET_NAME)
                    .key(fileName)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        }
    }

    private static void deleteBucket() {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                .bucket(TEST_BFD_BUCKET_NAME)
                .build();

        s3Client.deleteBucket(deleteBucketRequest);
    }
}
