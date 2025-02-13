package gov.cms.ab2d.common.health;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemCheckTest {

    @Test
    void canWriteFile() throws IOException {
        assertFalse(FileSystemCheck.canWriteFile(null, false));
        assertTrue(FileSystemCheck.canWriteFile(System.getProperty("java.io.tmpdir"), false));
        assertTrue(FileSystemCheck.canWriteFile(".", false));
        assertFalse(FileSystemCheck.canWriteFile("/notarealdir", false));
        String newTestDir = "/tmp/healthTestDir";
        assertTrue(FileSystemCheck.canWriteFile(newTestDir, true));
        Files.deleteIfExists(Path.of(newTestDir));
    }

    @Test
    @Disabled("Assertion for 'FileSystemCheck.canWriteFile' fails in GitHub test runner (but not locally)")
    void unableToWriteToDir() {
        String randomDirName = RandomStringUtils.randomAlphabetic(20);
        File newDir = new File("." + File.separator + randomDirName);
        assertTrue(newDir.mkdir());
        // Windows does not support the ability to turn off creating files in a directory
        if (!SystemUtils.IS_OS_WINDOWS) {
            assertTrue(newDir.setReadOnly());
            // TODO investigate why this fails in the GitHub runner
            assertFalse(FileSystemCheck.canWriteFile(randomDirName, false));
        }
        assertTrue(newDir.delete());
    }

    @Test
    void getRandomFileName() {
        for (int i = 0; i < 100; i++) {
            String file1 = FileSystemCheck.getRandomFileName(10, "txt");
            String file2 = FileSystemCheck.getRandomFileName(10, "txt");
            assertNotEquals(file1, file2);
            byte firstByte = file1.getBytes()[0];
            byte firstByte2= file2.getBytes()[0];
            byte secondByte = file1.getBytes()[1];
            byte secondByte2= file2.getBytes()[1];
            assertTrue((firstByte >= 'A' && firstByte <= 'Z') || (firstByte >= 'a' && firstByte <='z'));
            assertTrue((firstByte2 >= 'A' && firstByte2 <= 'Z') || (firstByte2 >= 'a' && firstByte2 <='z'));
            assertTrue((secondByte >= 'A' && secondByte <= 'Z') || (secondByte >= 'a' && secondByte <='z') || (secondByte >= '0' && secondByte <= '9'));
            assertTrue((secondByte2 >= 'A' && secondByte2 <= 'Z') || (secondByte2 >= 'a' && secondByte2 <='z') || (secondByte2 >= '0' && secondByte2 <= '9'));
        }
        assertEquals(".txt", FileSystemCheck.getRandomFileName(0, "txt"));
        assertEquals(12, FileSystemCheck.getRandomFileName(8, "txt").length());
        assertTrue(FileSystemCheck.getRandomFileName(8, "txt").endsWith(".txt"));
    }
}