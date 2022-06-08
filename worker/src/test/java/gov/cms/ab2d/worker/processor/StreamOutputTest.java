package gov.cms.ab2d.worker.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static gov.cms.ab2d.aggregator.FileOutputType.DATA;
import static gov.cms.ab2d.aggregator.FileOutputType.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StreamOutputTest {
    @Test
    void testStreamOutput(@TempDir File tmpDir) throws IOException {
        Path tmpFile = Path.of(tmpDir.getAbsolutePath(), "test.ndjson");
        Files.writeString(tmpFile, "abc");

        StreamOutput output = new StreamOutput(tmpFile.toFile(), DATA);
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", output.getChecksum());
        assertEquals(3, output.getFileLength());
        assertEquals(tmpFile.toFile().getName(), output.getFilePath());
        assertEquals(DATA, output.getType());

        Path tmpFile2 = Path.of(tmpDir.getAbsolutePath(), "test2.ndjson");
        StreamOutput output2 = new StreamOutput("test2.ndjson", output.getChecksum(), 6, ERROR);
        assertEquals(6, output2.getFileLength());
        assertEquals("test2.ndjson", output2.getFilePath());
        assertEquals(ERROR, output2.getType());
        assertThrows(UncheckedIOException.class, () -> StreamOutput.generateChecksum(tmpFile2.toFile()));
    }
}