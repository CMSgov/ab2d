package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.model.CoverageV3Periods;
import gov.cms.ab2d.coverage.model.YearMonthRecord;
import lombok.val;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

public class CountBeneficiariesByCoveragePeriods extends CoverageV3BaseQuery {

    public CountBeneficiariesByCoveragePeriods(DataSource dataSource) {
        super(dataSource);
    }
    
    private static final String COUNT_BENEFICIARIES_WITHOUT_OPTOUT =
    """
    select count(distinct patient_id) from (
        select * from v3.coverage_v3
            where contract = :contract
        union
        select * from  v3.coverage_v3_historical
            where contract = :contract
    )
    where (year,month) in (:historicalAndRecentCoveragePeriods)
    """;

    private static final String COUNT_BENEFICIARIES_WITH_OPTOUT =
    """
    select count(distinct patient_id) from
    (
       select * from v3.coverage_v3
           where contract = :contract
       union
       select * from  v3.coverage_v3_historical
           where contract = :contract
    ) as union_results
    join current_mbi on union_results.current_mbi = current_mbi.mbi
    where (year,month) in (:historicalAndRecentCoveragePeriods)
        and current_mbi is not null
        and share_data is not false
    """;

    public int countBeneficiaries(
        final String contract,
        final CoverageV3Periods periods,
        final boolean optOutOn
    ) {

        val query = optOutOn ? COUNT_BENEFICIARIES_WITH_OPTOUT : COUNT_BENEFICIARIES_WITHOUT_OPTOUT;

        val historicalAndRecentCoveragePeriods = new ArrayList<YearMonthRecord>(periods.getHistoricalCoverage().size() + periods.getRecentCoverage().size());
        historicalAndRecentCoveragePeriods.addAll(periods.getHistoricalCoverage());
        historicalAndRecentCoveragePeriods.addAll(periods.getRecentCoverage());

        val parameters = new MapSqlParameterSource()
                .addValue("contract", contract)
                .addValue("historicalAndRecentCoveragePeriods", toSqlParameters(historicalAndRecentCoveragePeriods));

        val template = new NamedParameterJdbcTemplate(this.dataSource);
        return DataAccessUtils.intResult(template.queryForList(query, parameters, Integer.class));
    }

    protected List<Object[]> toSqlParameters(List<YearMonthRecord> records) {
        return records.stream()
        .map(yearMonthRecord -> new Object[]{
                yearMonthRecord.getYear(),
                yearMonthRecord.getMonth()
        })
        .toList();
    }

}
