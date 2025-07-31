package gov.cms.ab2d.attributiondatashare;

import io.findify.s3mock.S3Mock;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;

import static gov.cms.ab2d.attributiondatashare.AttributionDataShareConstants.*;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class S3MockAPIExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static final S3Mock API = S3Mock.create(8001, "/tmp/s3");
    public static S3AsyncClient S3_CLIENT;
    private static boolean STARTED = false;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (!STARTED) {
            STARTED = true;
            context.getRoot().getStore(GLOBAL).put("S3MockAPIExtension", this);

            API.start();

            S3_CLIENT = S3AsyncClient.crtBuilder()
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
                .bucket(getBucketName())
                .build();

        S3_CLIENT.createBucket(bucketRequest);
    }

    public static boolean isObjectExists(String fileName) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(getUploadPath() + fileName)
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
                    .bucket(getBucketName())
                    .key(getUploadPath() + fileName)
                    .build();
            S3_CLIENT.deleteObject(deleteObjectRequest);
        }
    }

    private static void deleteBucket() {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                .bucket(getBucketName())
                .build();

        S3_CLIENT.deleteBucket(deleteBucketRequest);
    }

    public static String getBucketName() {
        return System.getenv(BUCKET_NAME_PROP);
    }

    public static String getUploadPath() {
        return System.getenv(UPLOAD_PATH_PROP) + "/";
    }
}
