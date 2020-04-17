package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.SpringBootApp;
import gov.cms.ab2d.eventlogger.events.*;
import gov.cms.ab2d.eventlogger.reports.sql.DeleteObjects;
import gov.cms.ab2d.eventlogger.reports.sql.LoadObjects;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
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
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
public class AllMapperEventTest {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    SqlEventLogger sqlEventLogger;

    @Autowired
    LoadObjects loadObjects;

    @Autowired
    DeleteObjects deleteObjects;

    @TempDir
    Path tmpDir;

    @Test
    void exceptionApiRequestTests() {
        assertThrows(EventLoggingException.class, () ->
                new ApiRequestEventMapper(null).log(new ErrorEvent()));
    }

    @Test
    void logApiRequest() {
        ApiRequestEvent jsce = new ApiRequestEvent("laila", "job123", "http://localhost",
                "127.0.0.1", "token", "123");
        sqlEventLogger.log(jsce);
        long id = jsce.getId();
        OffsetDateTime val = jsce.getTimeOfEvent();
        List<LoggableEvent> events = loadObjects.loadAllApiRequestEvent();
        assertEquals(1, events.size());
        ApiRequestEvent event = (ApiRequestEvent) events.get(0);
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), jsce.getId());
        assertEquals("laila", event.getUser());
        assertEquals("job123", event.getJobId());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        assertEquals("http://localhost", event.getUrl());
        assertEquals("127.0.0.1", event.getIpAddress());
        assertEquals(UtilMethods.hashIt("token"), event.getTokenHash());
        assertEquals("123", event.getRequestId());
        deleteObjects.deleteAllApiRequestEvent();
        events = loadObjects.loadAllApiRequestEvent();
        assertEquals(0, events.size());
    }

    @Test
    void exceptionApiResponseTests() {
        assertThrows(EventLoggingException.class, () ->
                new ApiResponseEventMapper(null).log(new ErrorEvent()));
    }

    @Test
    void logApiResponse() {
        ApiResponseEvent jsce = new ApiResponseEvent("laila", "job123", HttpStatus.NOT_FOUND,
                "Not Found", "Description", "123");
        sqlEventLogger.log(jsce);
        long id = jsce.getId();
        OffsetDateTime val = jsce.getTimeOfEvent();
        List<LoggableEvent> events = loadObjects.loadAllApiResponseEvent();
        assertEquals(1, events.size());
        ApiResponseEvent event = (ApiResponseEvent) events.get(0);
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), jsce.getId());
        assertEquals("laila", event.getUser());
        assertEquals("job123", event.getJobId());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        assertEquals("Description", event.getDescription());
        assertEquals("Not Found", event.getResponseString());
        assertEquals(404, event.getResponseCode());
        assertEquals("123", event.getRequestId());
        deleteObjects.deleteAllApiResponseEvent();
        events = loadObjects.loadAllApiResponseEvent();
        assertEquals(0, events.size());
    }

    @Test
    void exceptionContractSearchTests() {
        assertThrows(EventLoggingException.class, () ->
                new ContractBeneSearchEventMapper(null).log(new ErrorEvent()));
    }

    @Test
    void logContractSearch() {
        ContractBeneSearchEvent cbse = new ContractBeneSearchEvent(
                "laila", "jobIdVal", "Contract123", 100, 95, 3, 2);
        sqlEventLogger.log(cbse);
        long id = cbse.getId();
        OffsetDateTime val = cbse.getTimeOfEvent();
        List<LoggableEvent> events = loadObjects.loadAllContractBeneSearchEvent();
        assertEquals(1, events.size());
        ContractBeneSearchEvent event = (ContractBeneSearchEvent) events.get(0);
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), cbse.getId());
        assertEquals("laila", event.getUser());
        assertEquals("jobIdVal", event.getJobId());
        assertEquals("Contract123", event.getContractNumber());
        assertEquals(100, event.getNumInContract());
        assertEquals(95, event.getNumSearched());
        assertEquals(3, event.getNumOptedOut());
        assertEquals(2, event.getNumErrors());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        deleteObjects.deleteAllContractBeneSearchEvent();
        events = loadObjects.loadAllContractBeneSearchEvent();
        assertEquals(0, events.size());
    }

    @Test
    void exceptionErrorEventTests() {
        assertThrows(EventLoggingException.class, () ->
                new ErrorEventMapper(null).log(new FileEvent()));
    }

    @Test
    void logErrorEvent() {
        ErrorEvent jsce = new ErrorEvent("laila", "job123", ErrorEvent.ErrorType.CONTRACT_NOT_FOUND,
                "Description");
        sqlEventLogger.log(jsce);
        long id = jsce.getId();
        OffsetDateTime val = jsce.getTimeOfEvent();
        List<LoggableEvent> events = loadObjects.loadAllErrorEvent();
        assertEquals(1, events.size());
        ErrorEvent event = (ErrorEvent) events.get(0);
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), jsce.getId());
        assertEquals("laila", event.getUser());
        assertEquals("job123", event.getJobId());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        assertEquals(ErrorEvent.ErrorType.CONTRACT_NOT_FOUND, event.getErrorType());
        assertEquals("Description", event.getDescription());
        deleteObjects.deleteAllErrorEvent();
        events = loadObjects.loadAllErrorEvent();
        assertEquals(0, events.size());
    }

    @Test
    void exceptionFileEventTests() {
        assertThrows(EventLoggingException.class, () ->
                new FileEventMapper(null).log(new ErrorEvent()));
    }

    @Test
    void logFileEvent() throws IOException {
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
        deleteObjects.deleteAllFileEvent();
        events = loadObjects.loadAllFileEvent();
        assertEquals(0, events.size());
    }

    @Test
    void exceptionJobStatusTests() {
        assertThrows(EventLoggingException.class, () ->
                new JobStatusChangeEventMapper(null).log(new FileEvent()));
    }

    @Test
    void logJobStatus() {
        JobStatusChangeEvent jsce = new JobStatusChangeEvent("laila", "job123", "IN_PROGRESS",
                "FAILED", "Description");
        sqlEventLogger.log(jsce);
        long id = jsce.getId();
        OffsetDateTime val = jsce.getTimeOfEvent();
        List<LoggableEvent> events = loadObjects.loadAllJobStatusChangeEvent();
        assertEquals(1, events.size());
        JobStatusChangeEvent event = (JobStatusChangeEvent) events.get(0);
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), jsce.getId());
        assertEquals("laila", event.getUser());
        assertEquals("job123", event.getJobId());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        assertEquals("FAILED", event.getNewStatus());
        assertEquals("IN_PROGRESS", event.getOldStatus());
        assertEquals("Description", event.getDescription());
        deleteObjects.deleteAllJobStatusChangeEvent();
        events = loadObjects.loadAllJobStatusChangeEvent();
        assertEquals(0, events.size());
    }

    @Test
    void exceptionReloadTests() {
        assertThrows(EventLoggingException.class, () ->
                new ReloadEventMapper(null).log(new ErrorEvent()));
    }

    @Test
    void logReload() {
        ReloadEvent cbse = new ReloadEvent(null, ReloadEvent.FileType.CONTRACT_MAPPING,
                "filename", 10);
        sqlEventLogger.log(cbse);
        long id = cbse.getId();
        OffsetDateTime val = cbse.getTimeOfEvent();
        List<LoggableEvent> events = loadObjects.loadAllReloadEvent();
        assertEquals(1, events.size());
        ReloadEvent event = (ReloadEvent) events.get(0);
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), cbse.getId());
        assertNull(event.getUser());
        assertNull(event.getJobId());
        assertEquals("filename", event.getFileName());
        assertEquals(10, event.getNumberLoaded());
        assertEquals(ReloadEvent.FileType.CONTRACT_MAPPING, event.getFileType());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        deleteObjects.deleteAllReloadEvent();
        events = loadObjects.loadAllReloadEvent();
        assertEquals(0, events.size());
    }
}
