package gov.cms.ab2d.importer;

import lombok.extern.slf4j.Slf4j;
import net.snowflake.client.jdbc.SnowflakeBasicDataSource;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.IOException;
import java.io.StringReader;
import java.security.PrivateKey;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

@Slf4j
public class SnowflakeCoverageQueryService {

    private final String url;
    private final String user;
    private final String privateKeyPem;
    private final String role;
    private final String warehouse;
    private final String db;
    private final String schema;

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
          bene.bene_mbi_id        AS current_mbi
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

    public SnowflakeCoverageQueryService(
            String url,
            String user,
            String privateKeyPem,
            String role,
            String warehouse,
            String db,
            String schema
    ) {
        this.url = url;
        this.user = user;
        this.privateKeyPem = privateKeyPem;
        this.role = role;
        this.warehouse = warehouse;
        this.db = db;
        this.schema = schema;
    }

    public Connection open() throws Exception {
        Properties props = new Properties();
        props.put("user", user);
        props.put("privateKey", loadPrivateKey(privateKeyPem));
        props.put("authenticator", "SNOWFLAKE_JWT");
        props.put("role", role);
        props.put("warehouse", warehouse);
        props.put("db", db);
        props.put("schema", schema);

        String jdbcUrl = url.contains("?")
                ? url + "&authenticator=SNOWFLAKE_JWT"
                : url + "?authenticator=SNOWFLAKE_JWT";

        return DriverManager.getConnection(jdbcUrl, props);
    }

    public PreparedStatement prepare(Connection connection) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(SQL);
        ps.setFetchSize(10_000);
        return ps;
    }

    private PrivateKey loadPrivateKey(String pem) throws IOException {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object object = parser.readObject();
            if (object instanceof PrivateKeyInfo privateKeyInfo) {
                return new JcaPEMKeyConverter().getPrivateKey(privateKeyInfo);
            }
            throw new IllegalArgumentException("Unsupported private key format");
        }
    }
}