package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ContractBeneSearchEvent;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.*;
import java.time.OffsetDateTime;

public class ContractBeneSearchEventMapper extends SqlEventMapper {
    private JdbcTemplate template;

    public ContractBeneSearchEventMapper(JdbcTemplate template) {
        this.template = template;
    }

    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != ContractBeneSearchEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + ContractBeneSearchEvent.class.toString());
        }
        String query = "insert into event_bene_search " +
                " (time_of_event, user_id, job_id, contract_number, num_in_contract, num_searched, num_opted_out, num_errors) " +
                " values (?, ?, ?, ?, ?, ?, ?, ?)";

        ContractBeneSearchEvent be = (ContractBeneSearchEvent) event;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        template.update(connection -> {
            PreparedStatement ps = connection
                    .prepareStatement(query, new String[] { "id" });
            ps.setObject(1, UtilMethods.convertToUtc(be.getTimeOfEvent()));
            ps.setString(2, be.getUser());
            ps.setString(3, be.getJobId());
            ps.setString(4, be.getContractNumber());
            ps.setInt(5, be.getNumInContract());
            ps.setInt(6, be.getNumSearched());
            ps.setInt(7, be.getNumOptedOut());
            ps.setInt(8, be.getNumErrors());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            event.setId(keyHolder.getKey().longValue());
        }
    }

    @Override
    public ContractBeneSearchEvent mapRow(ResultSet resultSet, int i) throws SQLException {
        ContractBeneSearchEvent event = new ContractBeneSearchEvent();
        event.setId(resultSet.getLong("id"));
        event.setTimeOfEvent(resultSet.getObject("time_of_event", OffsetDateTime.class));
        event.setUser(resultSet.getString("user_id"));
        event.setJobId(resultSet.getString("job_id"));

        event.setContractNumber(resultSet.getString("contract_number"));
        event.setNumInContract(resultSet.getInt("num_in_contract"));
        event.setNumSearched(resultSet.getInt("num_searched"));
        event.setNumOptedOut(resultSet.getInt("num_opted_out"));
        event.setNumErrors(resultSet.getInt("num_errors"));
        return event;
    }
}
