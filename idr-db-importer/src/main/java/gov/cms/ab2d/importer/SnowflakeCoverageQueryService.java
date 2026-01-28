package gov.cms.ab2d.importer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

@Component
@Slf4j
public class SnowflakeCoverageQueryService {

    @Value("${app.snowflake.datasource.url}")
    private String sfUrl;

    @Value("${app.snowflake.user}")
    private String user;

    @Value("${app.snowflake.password}")
    private String password;

    @Value("${app.snowflake.role}")
    private String role;

    @Value("${app.snowflake.warehouse}")
    private String warehouse;

    @Value("${app.snowflake.db}")
    private String db;

    @Value("${app.snowflake.schema}")
    private String schema;

    // Your query, slight tweak: rename mbi->current_mbi and add historic_mbis
    private static final String SQL = """
            WITH month_series AS (
              SELECT DATEADD(month, -seq4(), DATE_TRUNC('month', CURRENT_DATE()))::DATE AS month_start
              FROM TABLE(GENERATOR(ROWCOUNT => 3))
            )
            SELECT
              bene.bene_xref_efctv_sk AS patient_id,
              elec.bene_cntrct_num    AS contract,
              YEAR(ms.month_start)    AS year,
              MONTH(ms.month_start)   AS month,
              bene.bene_mbi_id        AS current_mbi,
              NULL                    AS historic_mbis
            FROM month_series ms
            JOIN CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_MAPD_ENRLMT elec
              ON elec.bene_enrlmt_bgn_dt <= LAST_DAY(ms.month_start)
             AND elec.bene_enrlmt_end_dt >= ms.month_start
            JOIN CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_HSTRY bene
              ON elec.bene_sk = bene.bene_sk
            WHERE elec.bene_cntrct_num LIKE 'S%'
              AND elec.idr_ltst_trans_flg = 'Y'
              AND elec.idr_trans_obslt_ts > '9999-12-30'
              AND bene.idr_ltst_trans_flg = 'Y'
              AND bene.bene_xref_efctv_sk != 0
            ORDER BY patient_id, year, month
            """;

    public Connection open() throws Exception {
        return DriverManager.getConnection(sfUrl, user, password);
    }

    public PreparedStatement prepare(Connection conn) throws Exception {
        PreparedStatement ps = conn.prepareStatement(SQL);
        ps.setFetchSize(10_000); // stream in chunks
        return ps;
    }
}
