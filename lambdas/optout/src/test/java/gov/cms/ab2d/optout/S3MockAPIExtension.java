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
    public static S3Client S3_CLIENT;
    private static boolean STARTED = false;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (!STARTED) {
            STARTED = true;
            context.getRoot().getStore(GLOBAL).put("S3MockAPIExtension", this);

            API.start();

            S3_CLIENT = S3Client.builder()
                    .region(S3_REGION)
                    .endpointOverride(new URI(TEST_ENDPOINT))
                    .build();

            createBucket();
        }
    }

    @Override
    public void close() {
        deleteBucket();
        S3_CLIENT.close();
        API.stop();
    }

    private static void createBucket() {
        var bucketRequest = CreateBucketRequest.builder()
                .bucket(TEST_BFD_BUCKET_NAME)
                .build();

        S3_CLIENT.createBucket(bucketRequest);
    }

    public static void createFile(String content, String fileName) {
        var objectRequest = PutObjectRequest.builder()
                .bucket(TEST_BFD_BUCKET_NAME)
                .key(fileName)
                .build();

        S3_CLIENT.putObject(objectRequest, RequestBody.fromString(content));
    }

    public static boolean isObjectExists(String fileName) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(TEST_BFD_BUCKET_NAME)
                    .key(fileName)
                    .build();

            S3_CLIENT.headObject(headObjectRequest);
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
            S3_CLIENT.deleteObject(deleteObjectRequest);
        }
    }

    private static void deleteBucket() {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                .bucket(TEST_BFD_BUCKET_NAME)
                .build();

        S3_CLIENT.deleteBucket(deleteBucketRequest);
    }
}
