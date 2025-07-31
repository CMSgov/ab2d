package gov.cms.ab2d.optout;


import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.cms.ab2d.lambdalibs.lib.ParameterStoreUtil;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static gov.cms.ab2d.optout.OptOutConstants.*;

public class OptOutProcessor {
    private final LambdaLogger logger;
    public List<OptOutInformation> optOutInformationList;
    public boolean isRejected;
    private final String dbHost;

    ParameterStoreUtil parameterStore;

    public OptOutProcessor(LambdaLogger logger) {
        this.logger = logger;
        this.optOutInformationList = new ArrayList<>();
        this.dbHost = System.getenv("DB_HOST");
        var environment = System.getenv("ENV");
        var bfdRole = "/opt-out-import/ab2d/" + environment + "/bfd-bucket-role-arn";
        var dbUser = "/ab2d/" + environment + "/core/sensitive/database_user";
        var dbPassword = "/ab2d/" + environment + "/core/sensitive/database_password";
        parameterStore = ParameterStoreUtil.getParameterStore(bfdRole, dbUser, dbPassword);
        isRejected = false;
    }

    public OptOutResults process(String fileName, String bfdBucket, String endpoint) throws URISyntaxException {
        var optOutS3 = new OptOutS3(getS3Client(endpoint), fileName, bfdBucket, logger);
        processFileFromS3(optOutS3.openFileS3());
        updateOptOut();
        var name = optOutS3.createResponseOptOutFile(createResponseContent());
        logger.log("File with name " + name + " was uploaded to bucket: " + bfdBucket);
        if (!isRejected)
            optOutS3.deleteFileFromS3();
        return getOptOutResults();
    }

    public S3Client getS3Client(String endpoint) throws URISyntaxException {
        var client = S3Client.builder()
                .region(S3_REGION)
                .endpointOverride(new URI(endpoint));

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

            client.credentialsProvider(credentials);
        }
        return client.build();
    }

    public void processFileFromS3(BufferedReader reader) {
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(HEADER_RESP) && !line.startsWith(TRAILER_RESP)) {
                    createOptOutInformation(line);
                }
            }
        } catch (IOException ex) {
            logger.log("An error occurred during file processing. " + ex.getMessage());
        }
    }

    public void createOptOutInformation(String information) {
        List<String> mbis = parseMbis(information);
        boolean optOutFlag = extractOptOutFlag(information, mbis.size() > 1);

        mbis.forEach(mbi ->
                optOutInformationList.add(new OptOutInformation(mbi.trim(), optOutFlag))
        );
    }

    private List<String> parseMbis(String information) {
        if (information.contains(",")) {
            String mbi1 = information.substring(MBI_INDEX_START, MBI_INDEX_LENGTH);
            int secondStart = MBI_INDEX_LENGTH + 1;
            String mbi2 = information.substring(secondStart, secondStart + MBI_INDEX_LENGTH);
            return List.of(mbi1, mbi2);
        } else {
            String mbi = information.substring(MBI_INDEX_START, MBI_INDEX_LENGTH);
            return List.of(mbi);
        }
    }

    private boolean extractOptOutFlag(String information, boolean hasTwoMbis) {
        int flagPosition = hasTwoMbis
                // for two MBIs, flag is just after the second MBI
                ? MBI_INDEX_LENGTH + 1 + MBI_INDEX_LENGTH
                // for one MBI, flag is at the standard offset
                : OPTOUT_FLAG_INDEX;
        return information.charAt(flagPosition) == 'Y';
    }


    public void updateOptOut() {
        try (var dbConnection = DriverManager.getConnection(dbHost, parameterStore.getDbUser(), parameterStore.getDbPassword());
             var statement = dbConnection.prepareStatement(UPSERT_STATEMENT)) {
            for (var optOutInformation : optOutInformationList) {
                statement.setString(1, optOutInformation.getMbi());
                statement.setBoolean(2, optOutInformation.getOptOutFlag());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            Pattern cleanup = Pattern.compile(
                    "INSERT INTO[\\s\\S]*?ERROR",
                    Pattern.DOTALL
            );
            String raw = ex.getMessage();
            String sanitized = raw;
            if (raw != null && raw.contains("INSERT INTO")) {
                sanitized = cleanup.matcher(raw)
                        .replaceAll("INSERT INTO. ERROR");
            }

            logger.log("There is an insertion error " + sanitized);
            isRejected = true;
        }
    }

    public String createResponseContent() {
        var date = new SimpleDateFormat(EFFECTIVE_DATE_PATTERN).format(new Date());
        var responseContent = new StringBuilder()
                .append(AB2D_HEADER_CONF)
                .append(date)
                .append(LINE_SEPARATOR);
        var recordStatus = getRecordStatus();
        var effectiveDate = getEffectiveDate(date);

        for (var optOutResult : optOutInformationList) {
            responseContent.append(optOutResult.getMbi())
                    .append(effectiveDate)
                    .append((optOutResult.getOptOutFlag()) ? 'Y' : 'N')
                    .append(recordStatus)
                    .append(LINE_SEPARATOR);
        }
        var lastLine = new StringBuilder()
                .append(AB2D_TRAILER_CONF)
                .append(date)
                .append(String.format("%010d", optOutInformationList.size()));
        responseContent.append(lastLine);
        logger.log("File trailer: " + lastLine);
        return responseContent.toString();
    }

    public OptOutResults getOptOutResults() {
        int totalOptedIn = 0;
        int totalOptedOut = 0;

        try (var dbConnection = DriverManager.getConnection(dbHost, parameterStore.getDbUser(), parameterStore.getDbPassword());
             var statement = dbConnection.createStatement();
             ResultSet rs = statement.executeQuery(COUNT_STATEMENT)
        ) {
            while (rs.next()) {
                totalOptedIn = rs.getInt("optin");
                totalOptedOut = rs.getInt("optout");
            }

            int numberOptedIn = 0;
            int numberOptedOut = 0;

            for (OptOutInformation optOut : optOutInformationList) {
                if (optOut.getOptOutFlag()) {
                    numberOptedIn++;
                } else {
                    numberOptedOut++;
                }
            }
            return new OptOutResults(numberOptedIn, numberOptedOut, totalOptedIn, totalOptedOut);
        } catch (SQLException ex) {
           logger.log("There is an error " + ex.getMessage());
        }
        return null;
    }

    public String getRecordStatus() {
        return (isRejected) ? RecordStatus.REJECTED.toString() : RecordStatus.ACCEPTED.toString();
    }

    public String getEffectiveDate(String date) {
        return (isRejected) ? " ".repeat(EFFECTIVE_DATE_LENGTH) : date;
    }

}