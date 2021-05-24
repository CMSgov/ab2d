package gov.cms.ab2d.worker.processor;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A single file output from a stream implementation with the necessary details
 * to identify the file
 */
@Getter
@AllArgsConstructor
public class StreamOutput {

    private final String filePath;
    private final String checksum;
    private final long fileLength;
    private final Boolean error;
}
