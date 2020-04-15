package gov.cms.ab2d.worker.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        Assertions.assertTrue(directory.toFile().isDirectory());
    }

    @Test
    void testCreateDirectoryWhenDirectoryAlreadyExist_ThrowsException() throws IOException {
        Files.deleteIfExists(tmpEfsMountDir.toPath());
        final Path directory = cut.createDirectory(tmpEfsMountDir.toPath());
        Assertions.assertTrue(directory.toFile().isDirectory());

        var exceptionThrown = assertThrows(RuntimeException.class,
                () -> cut.createDirectory(tmpEfsMountDir.toPath()));

        assertThat(exceptionThrown.getMessage(), startsWith("Could not create output directory"));
    }
}
