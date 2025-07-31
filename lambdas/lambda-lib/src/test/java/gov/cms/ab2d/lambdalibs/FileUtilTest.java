package gov.cms.ab2d.lambdalibs;

import gov.cms.ab2d.lambdalibs.lib.FileUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class FileUtilTest {
    final Path FILE_PATH = Paths.get("/tmp/opt/");
    final String FILE_NAME = "testFile.txt";

    @Test
    void temporaryFileDeletionTest() throws IOException {
        Path path = Paths.get("/tmp/" + FILE_NAME);
        byte[] data = {1, 2, 3, 4, 5};
        Files.write(path, data);

        FileUtil.deleteDirectoryRecursion(path);
        assertFalse(Files.exists(path));
    }

    @Test
    void multipleFilesDeletionTest() throws IOException {
        String dirName = "mydir";
        Path dirPath = Paths.get(dirName);
        if (!Files.exists(dirPath)) {
            Files.createDirectory(dirPath);
        }
        Path path1 = Paths.get(dirName + "/"+ FILE_NAME);
        Path path2 = Paths.get(dirName + "/file.txt");
        byte[] data = {1, 2, 3, 4, 5};
        Files.write(path1, data);
        Files.write(path2, data);

        FileUtil.deleteDirectoryRecursion(dirPath);
        assertFalse(Files.exists(path1));
        assertFalse(Files.exists(path2));
    }

    @Test
    void directoryDeletionTest() throws IOException {
        Files.createDirectories(FILE_PATH);

        FileUtil.deleteDirectoryRecursion(FILE_PATH);
        assertFalse(Files.exists(FILE_PATH));
    }

    @Test
    void directoryNonExistDeletionTest() {
        assertDoesNotThrow(() -> FileUtil.deleteDirectoryRecursion(FILE_PATH));
    }
}
