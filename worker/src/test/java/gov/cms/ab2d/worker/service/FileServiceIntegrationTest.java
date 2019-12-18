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
        cut = new FileService();
    }


    @Test
    void createFile_shouldRecreateNewFile_whenFileAlreadyExists() throws IOException {
        final Path path = tmpEfsMountDir.toPath();
        final String output_filename = "contract_name.ndjson";

        // pre-create file & write some text into file
        final Path filePath = Path.of(path.toString(), output_filename);
        final Path outputfile1 = Files.createFile(filePath);

        final List<String> lines = List.of("Just", "another", "day", "in", "paradise");
        Files.write(outputfile1, lines);

        assertAll(
                () -> assertTrue(Files.exists(outputfile1)),
                () -> assertLinesMatch(lines, Files.readAllLines(outputfile1))
        );

        //createFile method will delete and recreate a new empty file with the same name.
        final Path outputfile2 = cut.createFile(path, output_filename);

        assertAll(
                () -> assertTrue(Files.exists(outputfile2)),
                () -> assertTrue(Files.readAllLines(outputfile2).isEmpty()),
                () -> assertThat(outputfile1, equalTo(outputfile2))
        );
    }


    @Test
    void testCreateDirectory() throws IOException {
        Files.deleteIfExists(tmpEfsMountDir.toPath());
        final Path directory = cut.createDirectory(tmpEfsMountDir.toPath());
        Assertions.assertTrue(directory.toFile().isDirectory());
    }


    @Test
    void testCreateFile() throws IOException {
        Files.deleteIfExists(tmpEfsMountDir.toPath());
        cut.createDirectory(tmpEfsMountDir.toPath());

        var path = Paths.get(tmpEfsMountDir.getPath());
        var file = cut.createFile(path, "filename.ndjson");
        Assertions.assertTrue(file.toFile().isFile());
    }

    @Test
    void testCreateFile_WhenDirectoryDoesNotExist_ThrowsIOException() throws IOException {
        var path = Paths.get(tmpEfsMountDir.getPath(), "non-existent-directory");

        var exceptionThrown = assertThrows(RuntimeException.class,
                () -> cut.createFile(path, "filename.ndjson"));

        assertThat(exceptionThrown.getMessage(), startsWith("Could not create output file"));
    }

    @Test
    void testAppendToFile() throws IOException {
        Files.deleteIfExists(tmpEfsMountDir.toPath());
        cut.createDirectory(tmpEfsMountDir.toPath());

        var path = Paths.get(tmpEfsMountDir.getPath());
        var file = cut.createFile(path, "filename.ndjson");
        Assertions.assertTrue(file.toFile().isFile());

        List<String> lines = Arrays.asList("One", "Two", "Three");
        var bytes = String.join(System.lineSeparator(), lines).getBytes();

        var byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.writeBytes(bytes);

        cut.appendToFile(file, byteArrayOutputStream);
        assertLinesMatch(lines, Files.readAllLines(file));
    }


}
