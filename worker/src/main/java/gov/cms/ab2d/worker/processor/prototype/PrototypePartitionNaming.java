package gov.cms.ab2d.worker.processor.prototype;

import java.util.regex.Pattern;

/**
 * Handles all the file naming and checkpoint naming
 */
final class PrototypePartitionNaming {

    /** File name suffix for the exported EOBS */
    static final String DATA_STREAM = "";

    /** File name suffix for the error files */
    static final String ERROR_STREAM = "_error";

    /** Spring Batch's byte offset suffix for the writer */
    static final String CURRENT_COUNT_SUFFIX = ".current.count";

    /** Spring Batch's lines-writer suffix for the writer */
    static final String WRITTEN_SUFFIX = ".written";

    private static final String READER_CURSOR_PREFIX = "beneficiary.reader.cursor.t";
    private static final String DATA_WRITER_PREFIX = "ndjsonDataWriter.p";
    private static final String ERROR_WRITER_PREFIX = "ndjsonErrorWriter.p";
    private static final String TOKEN_INFIX = ".t";
    private static final String NDJSON_SUFFIX = ".ndjson";

    /**
     * Look at a given execution context and figure out what token its last committed chunk belongs to.
     * This is how copy-forward knows which file to copy from.
     */
    static final Pattern DATA_WRITER_OFFSET_KEY = Pattern.compile(
            Pattern.quote(DATA_WRITER_PREFIX) + "\\d+" + Pattern.quote(TOKEN_INFIX) + "(\\d+)"
                    + Pattern.quote(CURRENT_COUNT_SUFFIX));

    private PrototypePartitionNaming() {
    }

    /**
     * @param streamSuffix {@link #DATA_STREAM} or {@link #ERROR_STREAM}
     */
    static String fileName(String contractNumber, int partitionIndex, long fenceToken, String streamSuffix) {
        return contractNumber + "_partition" + partitionIndex + "_t" + fenceToken + streamSuffix + NDJSON_SUFFIX;
    }

    static String dataWriterName(int partitionIndex, long fenceToken) {
        return DATA_WRITER_PREFIX + partitionIndex + TOKEN_INFIX + fenceToken;
    }

    static String errorWriterName(int partitionIndex, long fenceToken) {
        return ERROR_WRITER_PREFIX + partitionIndex + TOKEN_INFIX + fenceToken;
    }

    static String readerCursorKey(long fenceToken) {
        return READER_CURSOR_PREFIX + fenceToken;
    }

    /**
     * True for any reader or writer key this package owns, so that recovery can clean up
     * old executions.
     */
    static boolean isCheckpointKey(String key) {
        return key.startsWith(READER_CURSOR_PREFIX)
                || key.startsWith(DATA_WRITER_PREFIX)
                || key.startsWith(ERROR_WRITER_PREFIX);
    }
}
