package gov.cms.ab2d.attributiondatashare;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gov.cms.ab2d.attributiondatashare.AttributionDataShareConstants.*;

public class AttributionDataShareHelper {
    private final LambdaLogger logger;
    private final String fileName;
    private final String fileFullPath;

    public AttributionDataShareHelper(String fileName, String fileFullPath, LambdaLogger logger) {
        this.fileName = fileName;
        this.fileFullPath = fileFullPath;
        this.logger = logger;
    }

    void copyDataToFile(Connection connection) {
        String date = new SimpleDateFormat(EFFECTIVE_DATE_PATTERN).format(new Date());
        try (var stmt = connection.createStatement();
             var writer = new BufferedWriter(new FileWriter(fileFullPath, true))) {

            var rs = getExecuteQuery(stmt);

            writer.write(AB2D_HEADER_REQ + date);
            writer.newLine();
            long records = 0;
            while (rs.next()) {
                var line = getResponseLine(rs.getString("mbi"), rs.getDate("effective_date"), rs.getBoolean("share_data"));
                writer.write(line);
                writer.newLine();
                records++;
            }
            String lastLine = AB2D_TRAILER_REQ + date + String.format("%010d", records);
            writer.write(lastLine);
            logger.log("File trailer: " + lastLine);
        } catch (SQLException | IOException ex) {
            String errorMessage = "An error occurred while exporting data to a file. ";
            logger.log(errorMessage + ex.getMessage());
            throw new AttributionDataShareException(errorMessage, ex);
        }
    }

    String getResponseLine(String currentMbi, Date effectiveDate, Boolean optOutFlag) {
        var result = new StringBuilder();
        result.append(currentMbi);
        // Adding spaces to the end of a string to achieve the required position index
        if (currentMbi.length() < CURRENT_MBI_LENGTH)
            result.append(" ".repeat(Math.max(0, CURRENT_MBI_LENGTH - currentMbi.length())));

        if (effectiveDate != null) {
            result.append(new SimpleDateFormat(EFFECTIVE_DATE_PATTERN).format(effectiveDate));
            result.append((optOutFlag) ? 'Y' : 'N');
        }
        // Exactly 20 characters on each line.
        // If the effective date and flag are null, additional spaces should be added at the end of string.
        return String.format("%-20s", result);
    }

    public String uploadToS3(S3AsyncClient s3AsyncClient) {
        S3TransferManager transferManager = S3TransferManager.builder()
                .s3Client(s3AsyncClient)
                .build();

        var name = getUploadPath() + fileName;
        var bucketName = getBucketName();

        UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                .putObjectRequest(b -> b.bucket(bucketName).key(name))
                .addTransferListener(LoggingTransferListener.create())
                .source(Paths.get(fileFullPath))
                .build();

        FileUpload fileUpload = transferManager.uploadFile(uploadFileRequest);

        CompletedFileUpload uploadResult = fileUpload.completionFuture().join();
        logger.log("File with name: " + name + " was uploaded to bucket: " + bucketName);
        return uploadResult.response().eTag();
    }

    public String getBucketName() {
        return System.getenv(BUCKET_NAME_PROP);
    }

    public String getUploadPath() {
        return System.getenv(UPLOAD_PATH_PROP) + "/";
    }

    static ResultSet getExecuteQuery(Statement statement) throws SQLException {
        return statement.executeQuery(SELECT_STATEMENT);
    }

}
