package gov.cms.ab2d.optout;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import static gov.cms.ab2d.optout.OptOutConstants.S3_REGION;
import static gov.cms.ab2d.optout.OptOutConstantsTest.TEST_BFD_BUCKET_NAME;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class S3MockAPIExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static final DockerImageName IMAGE_VERSION = DockerImageName.parse("localstack/localstack:3.5.0");
    private static final LocalStackContainer API =
            new LocalStackContainer(IMAGE_VERSION).withServices(LocalStackContainer.Service.S3);

    private static S3Client s3Client;
    private static String testEndpoint;
    private static boolean started = false;

    public static S3Client getS3Client() {
        return s3Client;
    }

    public static String getTestEndpoint() {
        return testEndpoint;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!started) {
            started = true;
            context.getRoot().getStore(GLOBAL).put("S3MockAPIExtension", this);

            API.start();

            testEndpoint = API.getEndpointOverride(LocalStackContainer.Service.S3).toString();

            // LocalStack accepts any credentials; provide them so the SDK default
            // credentials chain resolves both here and inside the code under test.
            System.setProperty("aws.accessKeyId", API.getAccessKey());
            System.setProperty("aws.secretAccessKey", API.getSecretKey());

            s3Client = S3Client.builder()
                    .region(S3_REGION)
                    .endpointOverride(API.getEndpointOverride(LocalStackContainer.Service.S3))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(API.getAccessKey(), API.getSecretKey())))
                    .forcePathStyle(true)
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
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(TEST_BFD_BUCKET_NAME)
                .build();

        s3Client.listObjectsV2(listRequest).contents()
                .forEach(s3Object -> deleteFile(s3Object.key()));

        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                .bucket(TEST_BFD_BUCKET_NAME)
                .build();

        s3Client.deleteBucket(deleteBucketRequest);
    }
}
