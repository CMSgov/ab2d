package gov.cms.ab2d.coverage.repository;

import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.coverage.model.CoverageSearchEvent;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

/**
 *
 */
@Repository
public class CoverageDeltaRepository {
    public static final String COVERAGE_DELETED = "DELETED";
    public static final String COVERAGE_ADDED = "ADDED";

    /**
     * Calculate the difference between two sets of coverage from BFD for a given {@link gov.cms.ab2d.coverage.model.CoveragePeriod}
     * and insert the results into a records table.
     */
    private static final String INSERT_DELTAS =
            "INSERT INTO coverage_delta (bene_coverage_period_id, beneficiary_id , type, created) " +
            CoverageServiceRepository.SELECT_DELTA;

    private final DataSource dataSource;

    public CoverageDeltaRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Given an original set of coverage from BFD and an updated set of coverage from BFD
     * {@link gov.cms.ab2d.coverage.model.CoveragePeriod}, calculate the number of beneficiaries "ADDED"
     * by the newer search (currentSearch) and "DELETED" by not being included in the newer search.
     *
     * @param previousSearch an earlier search from a previous update that is now outdated by CCW loads to BFD
     * @param currentSearch the latest search with the most up-to-date enrollment in BFD
     */
    @Trace
    public void trackDeltas(CoverageSearchEvent previousSearch, CoverageSearchEvent currentSearch) {

        // Calculate benes in previous search that are not in current search
        // These benes were removed from the contract
        SqlParameterSource deletedParams = new MapSqlParameterSource()
                .addValue("search1", previousSearch.getId())
                .addValue("search2", currentSearch.getId())
                .addValue("type", COVERAGE_DELETED);

        // Calculate benes in current search that are not in current search
        // These benes were added to the contract
        SqlParameterSource addedParams = new MapSqlParameterSource()
                .addValue("search1", currentSearch.getId())
                .addValue("search2", previousSearch.getId())
                .addValue("type", COVERAGE_ADDED);

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

        template.update(INSERT_DELTAS, deletedParams);
        template.update(INSERT_DELTAS, addedParams);
    }
}
