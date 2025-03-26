package gov.cms.ab2d.worker.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static gov.cms.ab2d.aggregator.FileOutputType.*;
import static org.junit.jupiter.api.Assertions.*;

class StreamOutputTest {
    @Test
    void testStreamOutputData(@TempDir File tmpDir) throws IOException {
        Path tmpFile = Path.of(tmpDir.getAbsolutePath(), "test.ndjson");
        Files.writeString(tmpFile, "abc");

        final String checksumOriginal = StreamOutput.generateChecksum(tmpFile.toFile());
        StreamOutput output = new StreamOutput(tmpFile.toFile());

        // StreamOutput#getChecksum should return checksum of the original, uncompressed file NOT the compressed file
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", checksumOriginal);
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", output.getChecksum());

        assertEquals(3, output.getFileLength());
        assertEquals("test.ndjson.gz", output.getFilePath());
        assertEquals(DATA_COMPRESSED, output.getType());
        assertFalse(tmpFile.toFile().exists(), "Original DATA file should be deleted");
    }

    @Test
    void testStreamOutputError(@TempDir File tmpDir) throws Exception {
        Path tmpFile = Path.of(tmpDir.getAbsolutePath(), "test_error.ndjson");
        Files.writeString(tmpFile, "abcdef");

        final String checksumOriginal = StreamOutput.generateChecksum(tmpFile.toFile());
        StreamOutput output = new StreamOutput(tmpFile.toFile());

        // StreamOutput#getChecksum should return checksum of the original, uncompressed file NOT the compressed file
        assertEquals("bef57ec7f53a6d40beb640a780a639c83bc29ac8a9816f1fc6c5c6dcd93c4721", checksumOriginal);
        assertEquals("bef57ec7f53a6d40beb640a780a639c83bc29ac8a9816f1fc6c5c6dcd93c4721", output.getChecksum());

        assertEquals(6, output.getFileLength());
        assertEquals("test_error.ndjson.gz", output.getFilePath());
        assertEquals(ERROR_COMPRESSED, output.getType());
        assertFalse(tmpFile.toFile().exists(), "Original ERROR file should be deleted");

    }

}