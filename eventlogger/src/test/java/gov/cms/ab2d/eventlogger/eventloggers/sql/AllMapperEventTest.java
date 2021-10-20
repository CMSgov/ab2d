package gov.cms.ab2d.eventlogger.eventloggers.sql;

import gov.cms.ab2d.eventlogger.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.EventLoggingException;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.SpringBootApp;
import gov.cms.ab2d.eventlogger.events.*;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import static gov.cms.ab2d.eventlogger.utils.UtilMethods.hashIt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
public class AllMapperEventTest {
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    SqlEventLogger sqlEventLogger;

    @Autowired
    private LoggerEventRepository loggerEventRepository;

    @TempDir
    Path tmpDir;

    @Mock
    NamedParameterJdbcTemplate template;

    @AfterEach
    public void init() {
        loggerEventRepository.delete();
    }

    @Test
    void exceptionApiRequestTests() {
        assertThrows(EventLoggingException.class, () ->
                new ApiRequestEventMapper(null).log(new ErrorEvent()));
    }

    @Test
    void createError() {
        // There is no mapping. Make sure it doesn't create an issue
        BeneficiarySearchEvent bene = new BeneficiarySearchEvent();
        sqlEventLogger.log(bene);
        sqlEventLogger.updateAwsId("TEST", bene);
    }

    @Test
    void createErrorTable() {
        // There is no mapping. Make sure it doesn't create an issue
        BeneficiarySearchEvent bene = new BeneficiarySearchEvent();
        sqlEventLogger.updateAwsId("TEST", bene);
    }

    @Test
    void logApiRequest() {
        ApiRequestEvent jsce = new ApiRequestEvent("laila", "job123", "http://localhost",
                "127.0.0.1", "token", "123");
        jsce.setAwsId("ABC");

        sqlEventLogger.log(jsce);

        assertEquals("ab2d-dev", jsce.getEnvironment());

        OffsetDateTime val = jsce.getTimeOfEvent();
        List<LoggableEvent> events = loggerEventRepository.load(ApiRequestEvent.class);
        assertEquals(1, events.size());

        List<LoggableEvent> events2 = loggerEventRepository.load();
        assertEquals(events.size(), events2.size());

        ApiRequestEvent event = (ApiRequestEvent) events.get(0);
        assertEquals("ABC", event.getAwsId());

        jsce.setId(event.getId());
        assertEquals(event, jsce);

        assertEquals("ab2d-dev", event.getEnvironment());
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), jsce.getId());
        assertEquals("laila", event.getOrganization());
        assertEquals("job123", event.getJobId());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        assertEquals("http://localhost", event.getUrl());
        assertEquals("127.0.0.1", event.getIpAddress());
        assertEquals(hashIt("token"), event.getTokenHash());
        assertEquals("123", event.getRequestId());

        loggerEventRepository.delete(ApiRequestEvent.class);

        events = loggerEventRepository.load(ApiRequestEvent.class);
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
        assertEquals("ab2d-dev", jsce.getEnvironment());
        long id = jsce.getId();
        OffsetDateTime val = jsce.getTimeOfEvent();
        List<LoggableEvent> events = loggerEventRepository.load(ApiResponseEvent.class);
        assertEquals(1, events.size());
        List<LoggableEvent> events2 = loggerEventRepository.load();
        assertEquals(events.size(), events2.size());
        ApiResponseEvent event = (ApiResponseEvent) events.get(0);
        assertEquals("ab2d-dev", event.getEnvironment());
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), jsce.getId());
        assertEquals("laila", event.getOrganization());
        assertEquals("job123", event.getJobId());
        assertEquals(event, jsce);
        assertEquals("Description", event.getDescription());
        assertEquals("Not Found", event.getResponseString());
        assertEquals(404, event.getResponseCode());
        assertEquals("123", event.getRequestId());
        loggerEventRepository.delete(ApiResponseEvent.class);
        events = loggerEventRepository.load(ApiResponseEvent.class);
        assertEquals(0, events.size());
    }

    @Test
    void exceptionContractSearchTests() {
        assertThrows(EventLoggingException.class, () ->
                new ContractSearchEventMapper(null).log(new ErrorEvent()));
    }

    @Test
    void logContractSearch() {
        ContractSearchEvent cbse = new ContractSearchEvent(
                "laila", "jobIdVal", "Contract123", 100, 100, 100, 2, 1000, 1000, 1);
        sqlEventLogger.log(cbse);
        assertEquals("ab2d-dev", cbse.getEnvironment());
        long id = cbse.getId();
        OffsetDateTime val = cbse.getTimeOfEvent();
        List<LoggableEvent> events = loggerEventRepository.load(ContractSearchEvent.class);
        assertEquals(1, events.size());
        List<LoggableEvent> events2 = loggerEventRepository.load();
        assertEquals(events.size(), events2.size());

        ContractSearchEvent event = (ContractSearchEvent) events.get(0);
        assertEquals("ab2d-dev", event.getEnvironment());
        assertTrue(event.getId() > 0);
        assertEquals(event, cbse);
        assertEquals(event.getId(), cbse.getId());
        assertEquals("laila", event.getOrganization());
        assertEquals("jobIdVal", event.getJobId());
        assertEquals("Contract123", event.getContractNumber());

        assertEquals(100, event.getBenesExpected());
        assertEquals(100, event.getBenesQueued());
        assertEquals(100, event.getBenesSearched());
        assertEquals(2, event.getBenesErrored());

        assertEquals(1000, event.getEobsFetched());
        assertEquals(1000, event.getEobsWritten());
        assertEquals(1, event.getEobFiles());


        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        loggerEventRepository.delete(ContractSearchEvent.class);
        events = loggerEventRepository.load(ContractSearchEvent.class);
        assertEquals(0, events.size());
    }

    @Test
    void exceptionErrorEventTests() {
        assertThrows(EventLoggingException.class, () ->
                new ErrorEventMapper(null).log(new FileEvent()));
    }

    @Test
    void exceptionErrorEventSentLogTests() {
        ErrorEvent event = new ErrorEvent("user", "jobId", ErrorEvent.ErrorType.FILE_ALREADY_DELETED,
                "File Deleted");
        new ErrorEventMapper(template).log(event);
        assertEquals(event.getId(), 0);
    }

    @Test
    void logErrorEvent() {
        ErrorEvent jsce = new ErrorEvent("laila", "job123", ErrorEvent.ErrorType.CONTRACT_NOT_FOUND,
                "Description");
        sqlEventLogger.log(jsce);
        assertEquals("ab2d-dev", jsce.getEnvironment());
        long id = jsce.getId();
        OffsetDateTime val = jsce.getTimeOfEvent();
        List<LoggableEvent> events = loggerEventRepository.load(ErrorEvent.class);
        assertEquals(1, events.size());
        List<LoggableEvent> events2 = loggerEventRepository.load();
        assertEquals(events.size(), events2.size());
        ErrorEvent event = (ErrorEvent) events.get(0);
        assertEquals(event, jsce);
        assertEquals("ab2d-dev", event.getEnvironment());
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), jsce.getId());
        assertEquals("laila", event.getOrganization());
        assertEquals("job123", event.getJobId());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        assertEquals(ErrorEvent.ErrorType.CONTRACT_NOT_FOUND, event.getErrorType());
        assertEquals("Description", event.getDescription());
        loggerEventRepository.delete(ErrorEvent.class);
        events = loggerEventRepository.load(ErrorEvent.class);
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
        assertEquals("ab2d-dev", jsce.getEnvironment());
        long id = jsce.getId();
        OffsetDateTime val = jsce.getTimeOfEvent();
        List<LoggableEvent> events = loggerEventRepository.load(FileEvent.class);
        assertEquals(1, events.size());
        List<LoggableEvent> events2 = loggerEventRepository.load();
        assertEquals(events.size(), events2.size());
        FileEvent event = (FileEvent) events.get(0);
        assertEquals(event, jsce);
        assertEquals("ab2d-dev", event.getEnvironment());
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), jsce.getId());
        assertEquals("laila", event.getOrganization());
        assertEquals("job123", event.getJobId());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        assertEquals(hash, event.getFileHash());
        assertFalse(hash.isEmpty());
        assertEquals(tmpDir.toString() + "/testFile", event.getFileName());
        assertEquals(11, event.getFileSize());
        assertEquals(FileEvent.FileStatus.CLOSE, event.getStatus());
        f.delete();
        loggerEventRepository.delete(FileEvent.class);
        events = loggerEventRepository.load(FileEvent.class);
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
        assertEquals("ab2d-dev", jsce.getEnvironment());
        long id = jsce.getId();
        OffsetDateTime val = jsce.getTimeOfEvent();
        List<LoggableEvent> events = loggerEventRepository.load(JobStatusChangeEvent.class);
        assertEquals(1, events.size());
        List<LoggableEvent> events2 = loggerEventRepository.load();
        assertEquals(events.size(), events2.size());
        JobStatusChangeEvent event = (JobStatusChangeEvent) events.get(0);
        assertEquals(event, jsce);
        assertEquals("ab2d-dev", event.getEnvironment());
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), jsce.getId());
        assertEquals("laila", event.getOrganization());
        assertEquals("job123", event.getJobId());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        assertEquals("FAILED", event.getNewStatus());
        assertEquals("IN_PROGRESS", event.getOldStatus());
        assertEquals("Description", event.getDescription());
        loggerEventRepository.delete(JobStatusChangeEvent.class);
        events = loggerEventRepository.load(JobStatusChangeEvent.class);
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
        assertEquals("ab2d-dev", cbse.getEnvironment());
        long id = cbse.getId();
        OffsetDateTime val = cbse.getTimeOfEvent();
        List<LoggableEvent> events = loggerEventRepository.load(ReloadEvent.class);
        List<LoggableEvent> events2 = loggerEventRepository.load();
        assertEquals(events.size(), events2.size());
        assertEquals(1, events.size());
        ReloadEvent event = (ReloadEvent) events.get(0);
        assertEquals(event, cbse);
        assertEquals("ab2d-dev", event.getEnvironment());
        assertTrue(event.getId() > 0);
        assertEquals(event.getId(), cbse.getId());
        assertNull(event.getOrganization());
        assertNull(event.getJobId());
        assertEquals("filename", event.getFileName());
        assertEquals(10, event.getNumberLoaded());
        assertEquals(ReloadEvent.FileType.CONTRACT_MAPPING, event.getFileType());
        assertEquals(val.getNano(), event.getTimeOfEvent().getNano());
        loggerEventRepository.delete(ReloadEvent.class);
        events = loggerEventRepository.load(ReloadEvent.class);
        assertEquals(0, events.size());
    }

    @Test
    void loadTest() throws IOException {
        sqlEventLogger.log(new ApiRequestEvent("laila", "job123", "http://localhost",
                "127.0.0.1", "token", "123"));
        sqlEventLogger.log(new ApiResponseEvent("laila", "job123", HttpStatus.NOT_FOUND,
                "Not Found", "Description", "123"));
        sqlEventLogger.log(new ContractSearchEvent(
                "laila", "jobIdVal", "Contract123", 100, 100, 100, 2, 1000, 1000, 1));
        sqlEventLogger.log(new ErrorEvent("laila", "job123", ErrorEvent.ErrorType.CONTRACT_NOT_FOUND,
                "Description"));
        Path p = Path.of(tmpDir.toString(), "testFile");
        p.toFile().createNewFile();
        File f = p.toFile();
        FileUtils.writeStringToFile(f, "Hello World", UTF_8);
        sqlEventLogger.log(new FileEvent("laila", "job123", f, FileEvent.FileStatus.CLOSE));
        f.delete();
        sqlEventLogger.log(new JobStatusChangeEvent("laila", "job123", "IN_PROGRESS",
                "FAILED", "Description"));
        sqlEventLogger.log(new ReloadEvent(null, ReloadEvent.FileType.CONTRACT_MAPPING,
                "filename", 10));
        List<LoggableEvent> events = loggerEventRepository.load();
        assertEquals(7, events.size());
        loggerEventRepository.delete();
        events = loggerEventRepository.load();
        assertEquals(0, events.size());
        loggerEventRepository.delete();
    }

    @DisplayName("Block client ids from being logged")
    @Test
    void blockLoggingClientIds() {
        JobStatusChangeEvent jsce = new JobStatusChangeEvent("0123", "job123", "IN_PROGRESS",
                "FAILED", "Description");
        sqlEventLogger.log(jsce);

        List<LoggableEvent> events = loggerEventRepository.load(JobStatusChangeEvent.class);
        assertEquals(1, events.size());

        JobStatusChangeEvent event = (JobStatusChangeEvent) events.get(0);
        assertNull(event.getOrganization());
        loggerEventRepository.delete(JobStatusChangeEvent.class);
        events = loggerEventRepository.load(JobStatusChangeEvent.class);
        assertEquals(0, events.size());
    }}
