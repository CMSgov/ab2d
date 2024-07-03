package gov.cms.ab2d.common.health;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Does a health check on a database
 */
public final class DatabaseAvailable {

    private DatabaseAvailable() { }

    /**
     * Returns true if you can connect to the database and do a simple select
     *
     * @param datasource - the datasource to test
     * @return true if you can access the db, false otherwise
     */
    public static boolean isDbAvailable(DataSource datasource) {
        try {
            Connection conn = datasource.getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute("SELECT 1");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
