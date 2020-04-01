package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.SpringBootApp;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.eventlogger.reports.sql.LoadObjects;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
class FileEventMapperTest {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    SqlEventLogger sqlEventLogger;

    @Autowired
    LoadObjects loadObjects;

    @TempDir
    Path tmpDir;

    @Test
    void exceptionTests() {
        assertThrows(EventLoggingException.class, () ->
                new FileEventMapper(null).log(new ErrorEvent()));
    }

    @Test
    void log() throws IOException {
        Path p = Path.of(tmpDir.toString(), "testFile");
        p.toFile().createNewFile();
        File f = p.toFile();
        FileUtils.writeStringToFile(f, "Hello World", UTF_8);
        FileEvent jsce = new FileEvent("laila", "job123", f, FileEvent.FileStatus.CLOSE);
        String hash = jsce.getFileHash();
        sqlEventLogger.log(jsce);
        long id = jsce.getId();
        OffsetDateTime val = jsce.getTimeOfEvent();
        List<LoggableEvent> events = loadObjects.loadAllFileEvent();
        assertEquals(1, events.size());
        FileEvent event = (FileEvent) events.get(0);
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), jsce.getId());
        assertEquals("laila", event.getUser());
        assertEquals("job123", event.getJobId());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        assertEquals(hash, event.getFileHash());
        assertFalse(hash.isEmpty());
        assertEquals(tmpDir.toString() + "/testFile", event.getFileName());
        assertEquals(11, event.getFileSize());
        assertEquals(FileEvent.FileStatus.CLOSE, event.getStatus());
        f.delete();
    }
}