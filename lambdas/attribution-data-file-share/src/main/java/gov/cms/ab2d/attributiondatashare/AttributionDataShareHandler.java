package gov.cms.ab2d.attributiondatashare;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import gov.cms.ab2d.lambdalibs.lib.FileUtil;
import gov.cms.ab2d.lambdalibs.lib.ParameterStoreUtil;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gov.cms.ab2d.attributiondatashare.AttributionDataShareConstants.*;

public class AttributionDataShareHandler implements RequestStreamHandler {

    // Writes out a file to the FILE_PATH.
    // I.E: "P.AB2D.NGD.REQ.D240209.T1122001"

    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        logger.log("AttributionDataShare Lambda is started");

        var environment = System.getenv("ENV");
        String currentDate = new SimpleDateFormat(REQ_FILE_NAME_PATTERN).format(new Date());
        var prefix = (System.getenv(BUCKET_NAME_PROP).contains("prod")) ? "P" : "T";
        String fileName = prefix + REQ_FILE_NAME + currentDate;
        String fileFullPath = FILE_PATH + fileName;

        var bfdRole = "/opt-out-import/ab2d/" + environment + "/bfd-bucket-role-arn";
        var dbUser = "/ab2d/" + environment + "/core/sensitive/database_user";
        var dbPassword = "/ab2d/" + environment + "/core/sensitive/database_password";
        var dbHost = System.getenv("DB_HOST");

        var parameterStore = ParameterStoreUtil.getParameterStore(bfdRole, dbUser, dbPassword);

        AttributionDataShareHelper helper = helperInit(fileName, fileFullPath, logger);

        try (var dbConnection = DriverManager.getConnection(dbHost, parameterStore.getDbUser(), parameterStore.getDbPassword())) {

            helper.copyDataToFile(dbConnection);
            helper.uploadToS3(getAsyncS3Client(ENDPOINT, parameterStore));

        } catch (NullPointerException | SQLException ex) {
            throwAttributionDataShareException(logger, ex);
        } finally {
            FileUtil.deleteDirectoryRecursion(Paths.get(fileFullPath));
            logger.log("AttributionDataShare Lambda is completed");
        }
    }

    public S3AsyncClient getAsyncS3Client(String endpoint, ParameterStoreUtil parameterStore) {
        var client = S3AsyncClient.crtCreate();

        if (endpoint.equals(ENDPOINT)) {
            var stsClient = StsClient
                    .builder()
                    .region(S3_REGION)
                    .build();

            var request = AssumeRoleRequest
                    .builder()
                    .roleArn(parameterStore.getRole())
                    .roleSessionName("roleSessionName")
                    .build();

            var credentials = StsAssumeRoleCredentialsProvider
                    .builder()
                    .stsClient(stsClient)
                    .refreshRequest(request)
                    .build();

            client =
                    S3AsyncClient.crtBuilder()
                            .credentialsProvider(credentials)
                            .region(S3_REGION)
                            .targetThroughputInGbps(20.0)
                            .minimumPartSizeInBytes(8 * 1025 * 1024L)
                            .build();
        }
        return client;
    }

    AttributionDataShareHelper helperInit(String fileName, String fileFullPath, LambdaLogger logger) {
        return new AttributionDataShareHelper(fileName, fileFullPath, logger);
    }

    void throwAttributionDataShareException(LambdaLogger logger, Exception ex) {
        logger.log(ex.getMessage());
        throw new AttributionDataShareException(ex.getMessage(), ex);
    }


}
