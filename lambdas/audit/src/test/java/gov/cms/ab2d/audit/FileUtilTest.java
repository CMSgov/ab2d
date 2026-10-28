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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

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
    void deleteExpiredFilesOnlyDeletesOldNdjson() throws IOException {
        Path base = Files.createTempDirectory("audit");
        Path job = Files.createDirectories(base.resolve(RandomString.make(10)));
        Path ndjson = Files.writeString(job.resolve(RandomString.make(10) + ".ndjson"), "test");
        Path other = Files.writeString(job.resolve(RandomString.make(10) + ".ndjson2"), "test");
        TestContext context = new TestContext();

        assertEquals(0, FileUtil.deleteExpiredFiles(base.toString(), Instant.now().minus(1, ChronoUnit.HOURS), Set.of(), context.getLogger(), ".ndjson"));
        assertTrue(Files.exists(ndjson));

        assertEquals(1, FileUtil.deleteExpiredFiles(base.toString(), Instant.now().plus(1, ChronoUnit.HOURS), Set.of(), context.getLogger(), ".ndjson"));
        assertFalse(Files.exists(ndjson));
        assertTrue(Files.exists(other));
        assertTrue(Files.exists(job));
    }

    @Test
    void deleteExpiredFilesRemovesEmptiedDirectoriesButNotBase() throws IOException {
        Path base = Files.createTempDirectory("audit");
        Path job = Files.createDirectories(base.resolve(RandomString.make(10)));
        Path nested = Files.createDirectories(job.resolve("nested"));
        Path untouched = Files.createDirectories(base.resolve(RandomString.make(10)));
        for (int i = 0; i < 50; i++) {
            Files.writeString(job.resolve("S0000_partition" + i + "_t1.ndjson"), "test");
            Files.writeString(job.resolve("S0000_partition" + i + "_t1_error.ndjson"), "test");
        }
        Files.writeString(nested.resolve("nested.ndjson"), "test");
        Files.writeString(base.resolve("root.ndjson"), "test");
        TestContext context = new TestContext();

        assertEquals(102, FileUtil.deleteExpiredFiles(base.toString(), Instant.now().plus(1, ChronoUnit.HOURS), Set.of(), context.getLogger(), ".ndjson"));
        assertFalse(Files.exists(nested));
        assertFalse(Files.exists(job));
        assertTrue(Files.exists(untouched));
        assertTrue(Files.exists(base));
    }

    @Test
    void deleteExpiredFilesMatchesEverySuffix() throws IOException {
        Path base = Files.createTempDirectory("audit");
        Path job = Files.createDirectories(base.resolve(RandomString.make(10)));
        Path ndjson = Files.writeString(job.resolve(RandomString.make(10) + ".ndjson"), "test");
        Path gzipped = Files.writeString(job.resolve(RandomString.make(10) + ".ndjson.gz"), "test");
        Instant cutoff = Instant.now().plus(1, ChronoUnit.HOURS);
        TestContext context = new TestContext();

        assertEquals(1, FileUtil.deleteExpiredFiles(base.toString(), cutoff, Set.of(), context.getLogger(), ".ndjson.gz"));
        assertTrue(Files.exists(ndjson));
        assertFalse(Files.exists(gzipped));

        assertEquals(1, FileUtil.deleteExpiredFiles(base.toString(), cutoff, Set.of(), context.getLogger(), ".ndjson", ".ndjson.gz"));
        assertFalse(Files.exists(ndjson));
    }

    @Test
    void deleteExpiredFilesSkipsActiveJobs() throws IOException {
        Path base = Files.createTempDirectory("audit");
        String activeJob = UUID.randomUUID().toString();
        Path active = Files.createDirectories(base.resolve(activeJob));
        Path finished = Files.createDirectories(base.resolve(UUID.randomUUID().toString()));
        Path activeFile = Files.writeString(active.resolve("S0000_partition1_t1.ndjson"), "test");
        Files.writeString(finished.resolve("S0000_partition1_t1.ndjson"), "test");
        TestContext context = new TestContext();

        assertEquals(1, FileUtil.deleteExpiredFiles(base.toString(), Instant.now().plus(1, ChronoUnit.HOURS),
                Set.of(activeJob), context.getLogger(), ".ndjson"));
        assertTrue(Files.exists(activeFile));
        assertFalse(Files.exists(finished));
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
