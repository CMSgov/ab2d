package gov.cms.ab2d.optout;

import software.amazon.awssdk.regions.Region;

public class OptOutConstants {

    public static final String ENDPOINT = "https://s3.amazonaws.com";
    public static final Region S3_REGION = Region.US_EAST_1;
    public static final String HEADER_RESP = "HDR_BENEDATARSP";
    public static final String TRAILER_RESP = "TRL_BENEDATARSP";
    public static final String AB2D_HEADER_CONF = "HDR_BENECONFIRM";
    public static final String AB2D_TRAILER_CONF = "TRL_BENECONFIRM";
    public static final int MBI_INDEX_START = 0;
    public static final int MBI_INDEX_LENGTH = 11;
    public static final int OPTOUT_FLAG_INDEX = 11;
    public static final String EFFECTIVE_DATE_PATTERN = "yyyyMMdd";
    public static final int EFFECTIVE_DATE_LENGTH = 8;
    public static final String LINE_SEPARATOR = System.lineSeparator();
    public static final String CONF_FILE_NAME = "#EFT.ON.AB2D.NGD.CONF.";
    public static final String CONF_FILE_NAME_PATTERN = "'D'yyMMdd.'T'HHmmsss";
    public static final String UPSERT_STATEMENT = "INSERT INTO current_mbi (mbi, share_data, effective_date) \n" +
                    "VALUES (?, ?, current_date) \n" +
                    "ON CONFLICT (mbi) DO UPDATE \n" +
                    "SET share_data = EXCLUDED.share_data, \n" +
                    "effective_date = current_date;";
    public static final String COUNT_STATEMENT = "SELECT \n"+
        "COUNT(CASE WHEN share_data = 'true' THEN 1 END) AS optin, \n"+
        "COUNT(CASE WHEN share_data = 'false' THEN 1 END) AS optout \n"+
        "FROM current_mbi WHERE share_data IS NOT NULL";

    private OptOutConstants() {}
}
