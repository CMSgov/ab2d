package gov.cms.ab2d.coveragecounts;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import gov.cms.ab2d.databasemanagement.DatabaseUtil;
import gov.cms.ab2d.snsclient.messages.CoverageCountDTO;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES;


public class CoverageCountsHandler implements RequestStreamHandler {

    private final ObjectMapper mapper = JsonMapper.builder()
            .configure(ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, true)
            .build()
            .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(new JodaModule());

    private static final String INSERT_COUNT = "INSERT INTO lambda.coverage_counts\n" +
            "(CONTRACT_NUMBER, SERVICE, COUNT, YEAR, MONTH, CREATE_AT, COUNTED_AT) \n" +
            "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)";

    @SneakyThrows
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
        String eventString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        LambdaLogger logger = context.getLogger();
        logger.log(eventString);
        SNSEvent event = mapper.readValue(eventString, SNSEvent.class);
        context.getLogger()
                .log(event.toString());
        int[] id = new int[]{};

        Connection connection = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_COUNT)) {
            Optional.ofNullable(event.getRecords())
                    .orElse(new ArrayList<>())
                    .forEach(snsRecord -> processRecords(snsRecord, logger, stmt));
            id = stmt.executeBatch();
        } catch (Exception e) {
            log(e, logger);
        }

        outputStream.write(("{\"status\": \"ok\", \"Updated\":\"" + Arrays.toString(id) + "\" }").getBytes(StandardCharsets.UTF_8));
    }

    @SneakyThrows
    private void processRecords(SNSEvent.SNSRecord snsRecord, LambdaLogger logger, PreparedStatement stmt) {
        logger.log("checking records");
        Arrays.stream(mapper.readValue(snsRecord.getSNS()
                        .getMessage(), CoverageCountDTO[].class))
                .forEach(coverageCountDTO -> prepareCountInserts(coverageCountDTO, logger, stmt));
    }

    @SneakyThrows
    private void prepareCountInserts(CoverageCountDTO count, LambdaLogger logger, PreparedStatement stmt) {
        logger.log("populating obj");
        logger.log(count.getContractNumber());
        stmt.setString(1, count.getContractNumber());
        stmt.setString(2, count.getService());
        stmt.setInt(3, count.getCount());
        stmt.setInt(4, count.getYear());
        stmt.setInt(5, count.getMonth());
        stmt.setTimestamp(6, count.getCountedAt());
        stmt.addBatch();
    }

    private void log(Exception exception, LambdaLogger logger) {
        logger.log(exception.getMessage());
        throw new CoverageCountException(exception);
    }
}
