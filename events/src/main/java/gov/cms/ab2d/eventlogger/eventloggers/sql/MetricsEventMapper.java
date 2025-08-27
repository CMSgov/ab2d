package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventclient.events.MetricsEvent;
import gov.cms.ab2d.eventlogger.EventLoggingException;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Stream;

public class MetricsEventMapper extends SqlEventMapper {

    private final NamedParameterJdbcTemplate template;

    MetricsEventMapper(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    @Override
    void log(LoggableEvent event) {
        if (event.getClass() != MetricsEvent.class) {
            throw new EventLoggingException("Used " + event.getClass()
                    .toString() + " instead of " + MetricsEvent.class);
        }
        MetricsEvent metricsEvent = (MetricsEvent) event;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String query = "insert into event.event_metrics " +
                " (time_of_event, service, state_type, event_description, awsId, environment, job_id) " +
                " values (:time, :service, :statetype, :eventdescription, :awsId, :environment, :job)";


        SqlParameterSource parameters = super.addSuperParams(event)
                .addValue("service", metricsEvent.getService())
                .addValue("eventdescription", metricsEvent.getEventDescription())
                .addValue("statetype", metricsEvent.getStateType().toString());

        template.update(query, parameters, keyHolder);
        event.setId(Stream.of(keyHolder.getKeys())
                .filter(Objects::nonNull)
                .filter(f -> f.containsKey("id"))
                .map(f -> f.get("id"))
                .map(String::valueOf)
                .map(Long::valueOf)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Database did not generate key")));
    }

    @Override
    public MetricsEvent mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
        MetricsEvent event = MetricsEvent.builder()
                .service(rs.getString("service"))
                .service(rs.getString("event_description"))
                .stateType(MetricsEvent.State.valueOf(rs.getString("state_type")))
                .build();
        extractSuperParams(rs, event);
        return event;
    }

}
