package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.model.CoverageV3Periods;
import gov.cms.ab2d.coverage.model.YearMonthRecord;
import lombok.val;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

public class CountBeneficiariesByCoveragePeriods extends CoverageV3BaseQuery {

    public CountBeneficiariesByCoveragePeriods(DataSource dataSource) {
        super(dataSource);
    }

    private static final String COUNT_BENEFICIARIES_WITHOUT_OPTOUT =
        """
            select count(distinct patient_id) from (
                select * from v3.coverage_v3
                    where contract = :contract and (year,month) in (:recentCoveragePeriods)
                union
                select * from  v3.coverage_v3_historical
                    where contract = :contract and (year, month) in (:historicalCoveragePeriods)
            )
        """;

    private static final String COUNT_BENEFICIARIES_WITH_OPTOUT =
        """
           select count(distinct patient_id) from
           (
               select * from v3.coverage_v3
                   where contract = :contract and (year,month) in (:recentCoveragePeriods)
               union
               select * from  v3.coverage_v3_historical
                   where contract = :contract and (year, month) in (:historicalCoveragePeriods)
           ) as union_results
           join current_mbi on union_results.current_mbi = current_mbi.mbi
           where current_mbi is not null
           and share_data is not false
        """;


    public int countBeneficiaries(
            final String contract,
            final CoverageV3Periods periods,
            final boolean optOutOn
    ) {

        val query = optOutOn ? COUNT_BENEFICIARIES_WITHOUT_OPTOUT : COUNT_BENEFICIARIES_WITH_OPTOUT;

        val parameters = new MapSqlParameterSource()
                .addValue("contract", contract)
                .addValue("historicalCoveragePeriods", toSqlParameters(periods.getHistoricalCoverage()))
                .addValue("recentCoveragePeriods", toSqlParameters(periods.getRecentCoverage()));


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
