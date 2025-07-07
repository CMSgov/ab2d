package gov.cms.ab2d;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;

@Slf4j
public class Temp {

    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://localhost:5432/test_database";
        String username = "postgres";
        String password = "postgres";

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url, username, password);
            log.info("Current schema = {}", connection.getSchema());
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("show search_path;");
            resultSet.next();
            log.info("search_path = {}", resultSet.getString(1));
        } catch (SQLException e) {
            log.error("Oops", e);
        }


    }

}
