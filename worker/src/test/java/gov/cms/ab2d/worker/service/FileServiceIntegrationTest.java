package gov.cms.ab2d.worker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

public class FileServiceIntegrationTest {

    private FileService cut;


    @TempDir
    File tmpDir;

    @BeforeEach
    void setup() {
        cut = new FileService();
    }


    @Test
    void createFile_shouldRecreateNewFile_whenFileAlreadyExists() throws IOException {
        final Path path = tmpDir.toPath();
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


}
