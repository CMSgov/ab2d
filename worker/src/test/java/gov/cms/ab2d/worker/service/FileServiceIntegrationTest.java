package gov.cms.ab2d.worker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileServiceIntegrationTest {

    private FileService cut;

    @TempDir
    File tmpEfsMountDir;

    @BeforeEach
    void setup() {
        cut = new FileServiceImpl();
    }

    @Test
    void testCreateDirectory() throws IOException {
        Files.deleteIfExists(tmpEfsMountDir.toPath());
        final Path directory = cut.createDirectory(tmpEfsMountDir.toPath());
        assertTrue(directory.toFile().isDirectory());
    }

    @Test
    void testCreateDirectoryWhenDirectoryAlreadyExist_ThrowsException() throws IOException {
        Files.deleteIfExists(tmpEfsMountDir.toPath());
        final Path directory = cut.createDirectory(tmpEfsMountDir.toPath());
        assertTrue(directory.toFile().isDirectory());

        var exceptionThrown = assertThrows(RuntimeException.class,
                () -> cut.createDirectory(tmpEfsMountDir.toPath()));

        assertTrue(exceptionThrown.getMessage().startsWith("Could not create output directory"));
    }

    @Test
    void testGenerateChecksum() {
        assertThrows(UncheckedIOException.class, () -> cut.generateChecksum(new File("/invalid/file")));
    }
}
