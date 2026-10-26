package gov.cms.ab2d.audit;

import gov.cms.ab2d.lambdalibs.lib.PropertiesUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class JobStatusRepository {

    private static final String ACTIVE_JOBS_QUERY =
            "SELECT job_uuid FROM job WHERE status IN ('SUBMITTED', 'IN_PROGRESS')";

    private JobStatusRepository() {
    }

    /**
     * @return the id of all jobs that are currently active
     */
    public static Set<String> findActiveJobUuids() {
        Properties properties = PropertiesUtil.loadProps();
        try (Connection connection = DriverManager.getConnection(properties.getProperty("DB_URL"),
                properties.getProperty("DB_USERNAME"), properties.getProperty("DB_PASSWORD"));
             PreparedStatement statement = connection.prepareStatement(ACTIVE_JOBS_QUERY);
             ResultSet resultSet = statement.executeQuery()) {
            Set<String> jobUuids = new HashSet<>();
            while (resultSet.next()) {
                jobUuids.add(resultSet.getString("job_uuid"));
            }
            return jobUuids;
        } catch (SQLException exception) {
            throw new AuditException("Unable to look up active jobs", exception);
        }
    }
}
