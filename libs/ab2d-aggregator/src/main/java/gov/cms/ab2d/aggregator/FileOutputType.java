package gov.cms.ab2d.aggregator;

import java.io.File;
import java.util.stream.Stream;

/**
 * Taken from AB2D. Describes the different file endings for the data and error files created by the aggregator
 */
public enum FileOutputType {
    DATA(".ndjson"),
    DATA_COMPRESSED(".ndjson.gz"),
    ERROR("_error.ndjson"),
    ERROR_COMPRESSED("_error.ndjson.gz"),
    UNKNOWN("");

    private final String suffix;

    FileOutputType(String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }

    public static FileOutputType getFileType(File file) {
        return getFileType(file.getAbsolutePath());
    }

    public static FileOutputType getFileType(String file) {
        if (file == null) {
            return UNKNOWN;
        }

        return Stream.of(ERROR, ERROR_COMPRESSED, DATA, DATA_COMPRESSED)
                .filter(type -> file.endsWith(type.suffix))
                .findFirst()
                .orElse(UNKNOWN);
    }
}