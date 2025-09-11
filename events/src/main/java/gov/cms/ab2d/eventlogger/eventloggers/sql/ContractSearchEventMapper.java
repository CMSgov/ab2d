package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventclient.events.ContractSearchEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventlogger.EventLoggingException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.*;

public class ContractSearchEventMapper extends SqlEventMapper {
    private final NamedParameterJdbcTemplate template;

    ContractSearchEventMapper(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    @Override
    public void log(LoggableEvent event) {
        if (event.getClass() != ContractSearchEvent.class) {
            throw new EventLoggingException("Used " + event.getClass().toString() + " instead of " + ContractSearchEvent.class.toString());
        }
        ContractSearchEvent be = (ContractSearchEvent) event;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String query = "insert into event.event_bene_search " +
                " (time_of_event, organization, job_id, contract_number, benes_expected, benes_queued, benes_searched, " +
                "  benes_errored, benes_with_eobs, eobs_fetched, eobs_written, eob_files, aws_id, environment) " +
                " values (:time, :organization, :job, :contractNum, :benes_expected, :benes_queued, :benes_searched, " +
                "   :benes_errored, :benes_with_eobs, :eobs_fetched, :eobs_written, :eob_files, :awsId, :environment)";

        SqlParameterSource parameters = super.addSuperParams(event)
                .addValue("contractNum", be.getContractNumber())
                .addValue("benes_expected", be.getBenesExpected())
                .addValue("benes_queued", be.getBenesQueued())
                .addValue("benes_searched", be.getBenesSearched())
                .addValue("benes_errored", be.getBenesErrored())
                .addValue("benes_with_eobs", be.getBenesWithEobs())
                .addValue("eobs_fetched", be.getEobsFetched())
                .addValue("eobs_written", be.getEobsWritten())
                .addValue("eob_files", be.getEobFiles());

        template.update(query, parameters, keyHolder);
        event.setId(SqlEventMapper.getIdValue(keyHolder));
    }

    @Override
    public ContractSearchEvent mapRow(ResultSet resultSet, int i) throws SQLException {
        ContractSearchEvent event = new ContractSearchEvent();
        extractSuperParams(resultSet, event);

        event.setContractNumber(resultSet.getString("contract_number"));

        event.setBenesExpected(resultSet.getInt("benes_expected"));
        event.setBenesQueued(resultSet.getInt("benes_queued"));
        event.setBenesSearched(resultSet.getInt("benes_searched"));
        event.setBenesErrored(resultSet.getInt("benes_errored"));
        event.setBenesWithEobs(resultSet.getInt("benes_with_eobs"));

        event.setEobsFetched(resultSet.getInt("eobs_fetched"));
        event.setEobsWritten(resultSet.getInt("eobs_written"));
        event.setEobFiles(resultSet.getInt("eob_files"));

        return event;
    }
}
