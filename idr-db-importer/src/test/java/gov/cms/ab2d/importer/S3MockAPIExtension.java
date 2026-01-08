package gov.cms.ab2d.importer;

import io.findify.s3mock.S3Mock;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class S3MockAPIExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    // Adjust port/path as needed
    private static final int PORT = 8001;
    private static final String DATA_DIR = "/tmp/s3";
    private static final String ENDPOINT = "http://localhost:" + PORT;

    // For tests
    public static final Region S3_REGION = Region.US_EAST_1;
    public static final String TEST_BUCKET_NAME = "ab2dimportfroms3test-bucket";

    private static final S3Mock API = S3Mock.create(PORT, DATA_DIR);
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
                    .endpointOverride(new URI(ENDPOINT))
                    // If your existing tests work without forcePathStyle, keep it off.
                    // If you see bucket addressing issues, add: .forcePathStyle(true)
                    .build();

            createBucket();
        }
    }

    @Override
    public void close() {
        try {
            deleteBucket();
        } finally {
            if (S3_CLIENT != null) {
                S3_CLIENT.close();
            }
            API.stop();
        }
    }

    private static void createBucket() {
        S3_CLIENT.createBucket(CreateBucketRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .build());
    }

    public static void createFile(String content, String fileName) {
        S3_CLIENT.putObject(PutObjectRequest.builder()
                        .bucket(TEST_BUCKET_NAME)
                        .key(fileName)
                        .contentType("text/csv")
                        .build(),
                RequestBody.fromString(content));
    }

    public static boolean isObjectExists(String fileName) {
        try {
            S3_CLIENT.headObject(HeadObjectRequest.builder()
                    .bucket(TEST_BUCKET_NAME)
                    .key(fileName)
                    .build());
            return true;
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                return false;
            }
            throw ex;
        }
    }

    public static void deleteFile(String fileName) {
        if (isObjectExists(fileName)) {
            S3_CLIENT.deleteObject(DeleteObjectRequest.builder()
                    .bucket(TEST_BUCKET_NAME)
                    .key(fileName)
                    .build());
        }
    }

    private static void deleteBucket() {
        // If you create objects and want to delete bucket reliably, you usually must delete objects first.
        // We'll attempt bucket delete directly; if your tests leave objects behind, delete them first.
        S3_CLIENT.deleteBucket(DeleteBucketRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .build());
    }
}

