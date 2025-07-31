package gov.cms.ab2d.databasemanagement;


import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Testcontainers
class InvokeTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Test
    void database() throws SQLException {
        Connection conn = DatabaseUtil.getConnection();
        PreparedStatement stmt = conn.prepareStatement("SELECT 1 as res");
        ResultSet resultSet = stmt.executeQuery();
        resultSet.next();
        Assertions.assertEquals(1, resultSet.getInt("res"));
    }

    @Test
    void liquibase() throws SQLException, LiquibaseException {
        Connection conn = DatabaseUtil.setupDb(DatabaseUtil.getConnection());
        PreparedStatement stmt = conn.prepareStatement("SELECT 1 as res");
        ResultSet resultSet = stmt.executeQuery();
        resultSet.next();
        Assertions.assertEquals(1, resultSet.getInt("res"));
    }

}
