package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ContractBeneSearchEvent;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.*;

public class ContractBeneSearchEventMapper extends SqlEventMapper {
    private final NamedParameterJdbcTemplate template;

    ContractBeneSearchEventMapper(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != ContractBeneSearchEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + ContractBeneSearchEvent.class.toString());
        }
        ContractBeneSearchEvent be = (ContractBeneSearchEvent) event;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String query = "insert into event_bene_search " +
                " (time_of_event, user_id, job_id, contract_number, num_in_contract, num_searched, num_errors, aws_id, environment) " +
                " values (:time, :user, :job, :contractNum, :numInContract, :numSearched, :numErrors, :awsId, :environment)";

        SqlParameterSource parameters = super.addSuperParams(event)
                .addValue("contractNum", be.getContractNumber())
                .addValue("numInContract", be.getNumInContract())
                .addValue("numSearched", be.getNumSearched())
                .addValue("numErrors", be.getNumErrors());

        template.update(query, parameters, keyHolder);
        event.setId(SqlEventMapper.getIdValue(keyHolder));
    }

    @Override
    public ContractBeneSearchEvent mapRow(ResultSet resultSet, int i) throws SQLException {
        ContractBeneSearchEvent event = new ContractBeneSearchEvent();
        extractSuperParams(resultSet, event);

        event.setContractNumber(resultSet.getString("contract_number"));
        event.setNumInContract(resultSet.getInt("num_in_contract"));
        event.setNumSearched(resultSet.getInt("num_searched"));
        event.setNumErrors(resultSet.getInt("num_errors"));

        return event;
    }
}
