package gov.cms.ab2d.coverage.query;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;

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
		num_bytes BIGINT NOT NULL
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
	    num_bytes
	  )
	VALUES
	  (?, ?, ?, ?);
	""";

	private final JdbcTemplate template;

	public Metrics(DataSource dataSource) {
		super(dataSource);
		this.template = new JdbcTemplate(dataSource);
	}

	public void createMetricsTableIfNotExists(final String jobUuid) {
		// This could throw an exception if two threads are creating the table / indexes within the same time frame
		try {
			val query = MessageFormat.format(CREATE_METRICS_TABLE, jobUuid);
			template.execute(query);
		} catch (Exception e) {
			log.error("Error creating metrics table for {}", jobUuid);
		}
	}

	public void insertMetrics(String jobUuid, List<Metric> metrics) {
		val query = MessageFormat.format(INSERT_METRICS, jobUuid);
		template.batchUpdate(
			query,
			new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					val metric = metrics.get(i);
					ps.setLong(1, metric.requestNs);
					ps.setLong(2, metric.parseBundleNs);
					ps.setInt(3, metric.bundleCount);
					ps.setLong(4, metric.numBytes);
				}
				@Override
				public int getBatchSize() {
					return metrics.size();
				}
			}
		);
	}

	public record Metric(
			long requestNs,
			long parseBundleNs,
			int bundleCount,
			long numBytes
	) {}


}
