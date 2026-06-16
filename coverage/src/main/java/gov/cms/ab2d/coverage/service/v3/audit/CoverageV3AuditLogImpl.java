package gov.cms.ab2d.coverage.service.v3.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.util.PropertyConstants;
import gov.cms.ab2d.coverage.service.v3.CoverageV3SyncResult;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Optional;

@Service
@Slf4j
public class CoverageV3AuditLogImpl implements CoverageV3AuditLog {

	private static final String INSERT_AUDIT_LOG =
	"""
	INSERT INTO v3.coverage_v3_audit(action, result, contract, log, data)
	VALUES (:action, :result, :contract, :log, :data::jsonb)
	""";

	private final ObjectMapper mapper;
	private final NamedParameterJdbcTemplate template;
	private final PropertiesService propertiesService;

	public CoverageV3AuditLogImpl(DataSource dataSource, PropertiesService propertiesService) {
		this.mapper = new ObjectMapper();
		this.template = new NamedParameterJdbcTemplate(dataSource);
		this.propertiesService = propertiesService;
	}

	@Transactional
	@Override
	public void log(CoverageV3AuditAction action, CoverageV3SyncResult result, String contract, String logText, Object data) {
		if (!propertiesService.isToggleOn(PropertyConstants.V3_AUDIT_LOGGING_ENABLED, false)) {
			return;
		}

		MapSqlParameterSource parameters = null;
		try {
			val resultAsString = result != null
				? result.toString()
				: "";

			parameters = new MapSqlParameterSource()
				.addValue("action", action.toString())
				.addValue("result", resultAsString)
				.addValue("contract", Optional.ofNullable(contract).orElse(""))
				.addValue("log", Optional.ofNullable(logText).orElse(""))
				.addValue("data", toJson(data));

			template.update(INSERT_AUDIT_LOG, parameters);
		} catch (Exception e) {
			log.error("Error inserting audit log ({})", parameters, e);
		}
	}

	private String toJson(Object data) {
		if (data != null) {
			try {
				return mapper.writeValueAsString(data);
			} catch (JsonProcessingException e) {
				log.error("Error serializing to JSON", e);
			}
		}
		return "{}";
	}

}
