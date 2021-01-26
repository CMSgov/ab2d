package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.CoverageSearchEvent;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

@Repository
public class CoverageDeltaRepository {

    private static final String INSERT_DELTAS =
            "INSERT INTO coverage_delta (bene_coverage_period_id, beneficiary_id , type, created)" +
            CoverageServiceRepository.SELECT_DELTA;

    private final DataSource dataSource;

    public CoverageDeltaRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void trackDeltas(CoverageSearchEvent searchEvent1, CoverageSearchEvent searchEvent2) {

        SqlParameterSource deletedParams = new MapSqlParameterSource()
                .addValue("search1", searchEvent1.getId())
                .addValue("search2", searchEvent2.getId())
                .addValue("type", "DELETED");

        // Switch the order of the delta to get the added events.
        SqlParameterSource addedParams = new MapSqlParameterSource()
                .addValue("search1", searchEvent2)
                .addValue("search2", searchEvent1)
                .addValue("type", "ADDED");

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

        int numDeletion = template.update(INSERT_DELTAS, deletedParams);
        int numAdded = template.update(INSERT_DELTAS, addedParams);
    }
}
