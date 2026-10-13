package gov.cms.ab2d.importer;

import lombok.extern.slf4j.Slf4j;
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
import java.util.Collections;
import java.util.Properties;

@Slf4j
public class SnowflakeCoverageQueryService {

    @FunctionalInterface
    public interface PrivateKeyLoader {
        PrivateKey load(String pem) throws Exception;
    }

    private final String url;
    private final String user;
    private final String privateKeyPem;
    private final String role;
    private final String warehouse;
    private final String db;
    private final String schema;
    private final PrivateKeyLoader privateKeyLoader;

    // %1$s is the month_series body, %2$s an optional extra contract filter.
    private static final String SQL_TEMPLATE = """
            WITH month_series AS (
                     %1$s
                   )
               SELECT
                 bene.bene_xref_efctv_sk AS "patient_id",
                 elec.bene_cntrct_num    AS "contract",
                 YEAR(ms.month_start)    AS "year",
                 MONTH(ms.month_start)   AS "month",
                 bene.bene_mbi_id        AS "current_mbi"
               FROM month_series ms
               JOIN CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_MAPD_ENRLMT elec
                 ON elec.bene_enrlmt_bgn_dt <= LAST_DAY(ms.month_start)
                AND elec.bene_enrlmt_end_dt >= ms.month_start
               JOIN CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_HSTRY bene
                 ON elec.bene_sk = bene.bene_sk
               WHERE elec.bene_cntrct_num LIKE 'S%%'
                 AND elec.bene_enrlmt_pgm_type_cd IN (2, 3)
                 AND elec.idr_ltst_trans_flg = 'Y'
                 AND elec.idr_trans_obslt_ts > '9999-12-30'
                 AND bene.idr_ltst_trans_flg = 'Y'
                 AND bene.bene_xref_efctv_sk != 0%2$s
               ORDER BY "patient_id", "year", "month"
            """;

    // The current month and the 3 before it.
    private static final String DAILY_MONTH_SERIES = """
            SELECT DATEADD(month, -seq4(), DATE_TRUNC('month', CURRENT_DATE()))::DATE AS month_start
                     FROM TABLE(GENERATOR(ROWCOUNT => 4))""";

    static final String SQL = SQL_TEMPLATE.formatted(DAILY_MONTH_SERIES, "");

    private static final String BACKFILL_MONTH_SERIES = "SELECT TO_DATE(?) AS month_start";

    public SnowflakeCoverageQueryService(
            String url,
            String user,
            String privateKeyPem,
            String role,
            String warehouse,
            String db,
            String schema
    ) {
        this(
                url,
                user,
                privateKeyPem,
                role,
                warehouse,
                db,
                schema,
                SnowflakeCoverageQueryService::loadPrivateKeyFromPem
        );
    }

    SnowflakeCoverageQueryService(
            String url,
            String user,
            String privateKeyPem,
            String role,
            String warehouse,
            String db,
            String schema,
            PrivateKeyLoader privateKeyLoader
    ) {
        this.url = url;
        this.user = user;
        this.privateKeyPem = privateKeyPem;
        this.role = role;
        this.warehouse = warehouse;
        this.db = db;
        this.schema = schema;
        this.privateKeyLoader = privateKeyLoader;
    }

    public Connection open() throws Exception {
        Properties props = new Properties();
        props.put("user", user);
        props.put("privateKey", privateKeyLoader.load(privateKeyPem));
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

    /**
     * The daily extract restricted to one month and the given contracts. Enrollment is read as IDR has
     * it today, so it can differ slightly from what the daily extract held for that month at the time.
     */
    public PreparedStatement prepareBackfill(Connection connection, BackfillRequest request) throws SQLException {
        String placeholders = String.join(", ", Collections.nCopies(request.contracts().size(), "?"));
        String sql = SQL_TEMPLATE.formatted(
                BACKFILL_MONTH_SERIES,
                " AND elec.bene_cntrct_num IN (" + placeholders + ")"
        );

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setFetchSize(10_000);
        int i = 1;
        ps.setString(i++, request.month().atDay(1).toString());
        for (String contract : request.contracts()) {
            ps.setString(i++, contract);
        }
        return ps;
    }

    private static PrivateKey loadPrivateKeyFromPem(String pem) throws IOException {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object object = parser.readObject();
            if (object instanceof PrivateKeyInfo privateKeyInfo) {
                return new JcaPEMKeyConverter().getPrivateKey(privateKeyInfo);
            }
            throw new IllegalArgumentException("Unsupported private key format");
        }
    }
}