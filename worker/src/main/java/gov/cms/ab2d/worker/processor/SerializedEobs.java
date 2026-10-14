package gov.cms.ab2d.worker.processor;

import java.util.List;

/**
 * Serialized ndjson output of one beneficiary's EOBs. Contains both the successful and error lines, produced
 * by {@link EobNdjsonSerializer} and shared between the prototype and main code.
 */
public record SerializedEobs(List<String> dataLines, List<String> errorLines) {

    public SerializedEobs {
        dataLines = dataLines == null ? List.of() : dataLines;
        errorLines = errorLines == null ? List.of() : errorLines;
    }

    public boolean isEmpty() {
        return dataLines.isEmpty() && errorLines.isEmpty();
    }
}
