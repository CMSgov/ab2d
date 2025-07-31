package gov.cms.ab2d.audit;

import gov.cms.ab2d.testutils.TestContext;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.LINUX;
import static org.junit.jupiter.api.condition.OS.MAC;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.mockito.internal.verification.VerificationModeFactory.times;

class FileUtilTest {
    @Test
    void deleteFile() throws IOException {
        String path = System.getProperty("java.io.tmpdir") + "/" + RandomString.make(10) + ".test";
        Files.write(Paths.get(path), "test".getBytes(StandardCharsets.UTF_8));
        File file = new File(path);
        TestContext context = new TestContext();
        FileUtil.delete(file, context.getLogger());
        assertFalse(file.exists());
        Mockito.verify(context.getLogger(), times(1));
    }

    @Test
    void findFiles() throws IOException {
        String path = System.getProperty("java.io.tmpdir") + "/" + RandomString.make(10) + "/";
        new File(path).mkdirs();
        Files.write(Paths.get(path + RandomString.make(10) + ".ndjson"), "test".getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(path + RandomString.make(10) + ".ndjson2"), "test".getBytes(StandardCharsets.UTF_8));

        Set<File> files = new HashSet<>();
        FileUtil.findAllMatchingFilesAndParentDirs(path, files, ".ndjson");
        assertEquals(1, files.size());

    }

    @Test
    void improperRoot() {
        assertTrue(FileUtil.improperRoot("Δ:/fails"));
    }

    @Test
    @EnabledOnOs({LINUX, MAC})
    void improperRootNix() {
        assertTrue(FileUtil.improperRoot("Δ:/fails"));
    }

    @Test
    @EnabledOnOs({WINDOWS})
    void improperRootWin() {
        assertTrue(FileUtil.improperRoot("/fails"));
    }

    @Test
    @EnabledOnOs({LINUX, MAC})
    void properRootNix() throws IOException {
        assertFalse(FileUtil.improperRoot("/this/should/work"));
    }

    @Test
    @EnabledOnOs({WINDOWS})
    void properRootWin() {
        assertFalse(FileUtil.improperRoot("c:/this/Should/Work"));
    }


}
