package gov.cms.ab2d.coverage.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;

@Slf4j
public class BfdMetricsUtility extends CoverageV3BaseQuery {

	private static final String CREATE_METRICS_TABLE =
	"""
	CREATE TABLE IF NOT EXISTS v3."metrics_{0}" (
	    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
	    response_ns BIGINT NOT NULL,
	    parse_bundle_ns BIGINT NOT NULL,
	    bundle_count INT NOT NULL,
	    num_bytes BIGINT NOT NULL,
	    filter_ns BIGINT NOT NULL
	);
	""";

	private static final String INSERT_METRICS =
	"""
	INSERT INTO
	  v3."metrics_{0}" (
	    response_ns,
	    parse_bundle_ns,
	    bundle_count,
	    num_bytes,
	    filter_ns
	  )
	VALUES
	  (?, ?, ?, ?, ?);
	""";

	private final JdbcTemplate template;

	public BfdMetricsUtility(DataSource dataSource) {
		super(dataSource);
		this.template = new JdbcTemplate(dataSource);
	}

	// This can throw an exception if two or more threads are creating the table / indexes within the same time frame
	private synchronized void createMetricsTableIfNotExists(final String jobUuid) throws Exception {
		val query = MessageFormat.format(CREATE_METRICS_TABLE, jobUuid);
		template.execute(query);
	}

	public void addMetric(String jobUUid, BfdRequestMetric metric) {
		addMetric(jobUUid, new BfdRequestMetric[]{metric});
	}

	public void addMetric(String jobUuid, BfdRequestMetric[] metrics) {
		try {
			// this will fail the first time because the table won't exist -- better than calling 'create table if not exists' every single time
			insertMetrics(jobUuid, metrics);
		} catch (Exception e) {
			try {
				createMetricsTableIfNotExists(jobUuid);
				insertMetrics(jobUuid, metrics);
			} catch (Exception e2) {
				log.error("Error adding metrics for {}", jobUuid, e2);
			}
		}
	}

	private void insertMetrics(String jobUuid, BfdRequestMetric[] metrics) {
		val query = MessageFormat.format(INSERT_METRICS, jobUuid);
		template.batchUpdate(
				query,
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						val metric = metrics[i];
						ps.setLong(1, metric.responseNs);
						ps.setLong(2, metric.parseBundleNs);
						ps.setInt(3, metric.bundleCount);
						ps.setLong(4, metric.numBytes);
						ps.setLong(5, metric.filterNs);
					}
					@Override
					public int getBatchSize() {
						return metrics.length;
					}
				}
		);
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class BfdRequestMetric {
		private long responseNs;
		private long parseBundleNs;
		private int bundleCount;
		private long numBytes;
		private long filterNs;
	}

}
