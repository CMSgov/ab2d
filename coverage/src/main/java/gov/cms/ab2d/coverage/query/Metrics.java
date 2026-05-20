package gov.cms.ab2d.coverage.query;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;

@Slf4j
public class Metrics extends CoverageV3BaseQuery {

	private static final String CREATE_METRICS_TABLE =
	"""
	CREATE TABLE IF NOT EXISTS v3."metrics_{0}" (
		id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
		created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
		request_ns BIGINT NOT NULL,
		parse_bundle_ns BIGINT NOT NULL,
	    bundle_count INT NOT NULL,
		num_bytes BIGINT NOT NULL,
		filter_ns BIGINT NOT NULL
	);
	
	CREATE INDEX IF NOT EXISTS idx_id ON v3."metrics_{0}" (id);
	CREATE INDEX IF NOT EXISTS idx_request_ns ON v3."metrics_{0}" (request_ns);
	""";

	private static final String INSERT_METRICS =
	"""
	INSERT INTO
	  v3."metrics_{0}" (
	    request_ns,
	    parse_bundle_ns,
	    bundle_count,
	    num_bytes,
	    filter_ns
	  )
	VALUES
	  (?, ?, ?, ?, ?);
	""";

	private final JdbcTemplate template;

	public Metrics(DataSource dataSource) {
		super(dataSource);
		this.template = new JdbcTemplate(dataSource);
	}

	private void createMetricsTableIfNotExists(final String jobUuid) {
		// This could throw an exception if two or more threads are creating the table / indexes within the same time frame
		try {
			val query = MessageFormat.format(CREATE_METRICS_TABLE, jobUuid);
			template.execute(query);
		} catch (Exception e) {
			throw e;
		}
	}


	public void addMetricWithRetry(String jobUuid, Metric[] metrics) {
		try {
			// this will fail the first time because the table won't exist -- better than calling 'create table if not exists' every single time
			addMetric(jobUuid, metrics);
		} catch (Exception e) {
			try {
				createMetricsTableIfNotExists(jobUuid);
				addMetric(jobUuid, metrics);
			} catch (Exception e2) {
				log.error("Error adding metrics for {}", jobUuid, e2);
			}
		}
	}

	private void addMetric(String jobUuid, Metric[] metrics) {
		val query = MessageFormat.format(INSERT_METRICS, jobUuid);
		template.batchUpdate(
				query,
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						val metric = metrics[i];
						ps.setLong(1, metric.requestNs);
						ps.setLong(2, metric.parseBundleNs);
						ps.setInt(3, metric.bundleCount);
						ps.setLong(4, metric.numBytes);
						ps.setLong(5, metric.filterNs[0]);
					}
					@Override
					public int getBatchSize() {
						return metrics.length;
					}
				}
		);
	}

	public record Metric(
			long requestNs,
			long parseBundleNs,
			int bundleCount,
			long numBytes,
			long[] filterNs
	) {}


}
