package gov.cms.ab2d.attributiondatashare;

import software.amazon.awssdk.regions.Region;

public final class AttributionDataShareConstants {

    public static final String ENDPOINT = "https://s3.amazonaws.com";
    public static final String TEST_ENDPOINT = "http://127.0.0.1:8001";
    public static final Region S3_REGION = Region.US_EAST_1;
    public static final String FILE_PATH = "/tmp/";
    public static final String REQ_FILE_NAME = "#EFT.ON.AB2D.NGD.REQ.";
    public static final String REQ_FILE_NAME_PATTERN = "'D'yyMMdd.'T'HHmmsss";
    public static final String AB2D_HEADER_REQ = "HDR_BENEDATAREQ";
    public static final String AB2D_TRAILER_REQ = "TRL_BENEDATAREQ";
    public static final String SELECT_STATEMENT = "SELECT * FROM current_mbi";
    public static final int CURRENT_MBI_LENGTH = 11;
    public static final String EFFECTIVE_DATE_PATTERN = "yyyyMMdd";
    public static final String BUCKET_NAME_PROP = "S3_UPLOAD_BUCKET";
    public static final String UPLOAD_PATH_PROP = "S3_UPLOAD_PATH";

    private AttributionDataShareConstants() {
    }
}
