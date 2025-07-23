package gov.cms.ab2d.databasemanagement;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class DatabaseManagementHandler implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        // create the schema externally since liquibase also uses it for the log tables
        LambdaLogger logger = context.getLogger();
        Connection connection;
        try {
            connection = DatabaseUtil.getConnection();
            try (PreparedStatement stmt = connection
                    .prepareStatement("CREATE SCHEMA if not exists lambda")) {
                stmt.execute();
            }
            DatabaseUtil.setupDb(connection);
        } catch (Exception e) {
            logger.log(e.getMessage());
        }
        outputStream.write("{\"status\": \"database update complete\", \"Updated\":\"\" }".getBytes(StandardCharsets.UTF_8));
    }
}
