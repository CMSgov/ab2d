package gov.cms.ab2d.common.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemCheckTest {

    @Test
    void canWriteFile() throws IOException {
        assertTrue(FileSystemCheck.canWriteFile("/tmp", false));
        assertTrue(FileSystemCheck.canWriteFile(".", false));
        assertFalse(FileSystemCheck.canWriteFile("/notarealdir", false));
        assertFalse(FileSystemCheck.canWriteFile("/bin", false));
        String newTestDir = "/tmp/healthTestDir";
        assertTrue(FileSystemCheck.canWriteFile(newTestDir, true));
        Files.deleteIfExists(Path.of(newTestDir));
    }

    @Test
    void getRandomFileName() {
        assertNotEquals(FileSystemCheck.getRandomFileName(10, "txt"), FileSystemCheck.getRandomFileName(10, "txt"));
        assertNotEquals(FileSystemCheck.getRandomFileName(10, "txt"), FileSystemCheck.getRandomFileName(10, "txt"));
        assertNotEquals(FileSystemCheck.getRandomFileName(10, "txt"), FileSystemCheck.getRandomFileName(10, "txt"));
        assertNotEquals(FileSystemCheck.getRandomFileName(10, "txt"), FileSystemCheck.getRandomFileName(10, "txt"));
        assertNotEquals(FileSystemCheck.getRandomFileName(10, "txt"), FileSystemCheck.getRandomFileName(10, "txt"));
        assertNotEquals(FileSystemCheck.getRandomFileName(10, "txt"), FileSystemCheck.getRandomFileName(10, "txt"));
        assertNotEquals(FileSystemCheck.getRandomFileName(10, "txt"), FileSystemCheck.getRandomFileName(10, "txt"));
        assertNotEquals(FileSystemCheck.getRandomFileName(10, "txt"), FileSystemCheck.getRandomFileName(10, "txt"));
        assertNotEquals(FileSystemCheck.getRandomFileName(10, "txt"), FileSystemCheck.getRandomFileName(10, "txt"));
        assertNotEquals(FileSystemCheck.getRandomFileName(10, "txt"), FileSystemCheck.getRandomFileName(10, "txt"));
        assertNotEquals(FileSystemCheck.getRandomFileName(10, "txt"), FileSystemCheck.getRandomFileName(10, "txt"));
        assertNotEquals(FileSystemCheck.getRandomFileName(10, "txt"), FileSystemCheck.getRandomFileName(10, "txt"));
        assertNotEquals(FileSystemCheck.getRandomFileName(10, "txt"), FileSystemCheck.getRandomFileName(10, "txt"));
        assertNotEquals(FileSystemCheck.getRandomFileName(10, "txt"), FileSystemCheck.getRandomFileName(10, "txt"));
        assertEquals(12, FileSystemCheck.getRandomFileName(8, "txt").length());
        assertTrue(FileSystemCheck.getRandomFileName(8, "txt").endsWith(".txt"));
    }
}