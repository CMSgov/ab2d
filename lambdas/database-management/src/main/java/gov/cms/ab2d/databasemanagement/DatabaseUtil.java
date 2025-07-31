package gov.cms.ab2d.databasemanagement;

import gov.cms.ab2d.lambdalibs.lib.PropertiesUtil;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseUtil {
    private DatabaseUtil() {
    }

    public static Connection getConnection() {
        Properties properties = PropertiesUtil.loadProps();
        try {
            return DriverManager.getConnection(properties.get("DB_URL") + "", properties.get("DB_USERNAME") + "", properties.get("DB_PASSWORD") + "");
        }
        catch (SQLException ex){
            throw new DatabaseManagementException("Unable to get connection to ab2d database", ex);
        }
    }

    public static Connection setupDb(Connection connection) throws LiquibaseException {
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        Liquibase liquibase = new liquibase.Liquibase("db/changelog/changelog.yaml", new ClassLoaderResourceAccessor(), database);
        liquibase.update(new Contexts(), new LabelExpression());
        return connection;
    }

}
