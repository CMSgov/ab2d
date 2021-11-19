package gov.cms.ab2d.eventlogger.reports.sql;

import gov.cms.ab2d.eventlogger.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.SpringBootApp;
import gov.cms.ab2d.eventlogger.eventloggers.sql.SqlEventLogger;
import gov.cms.ab2d.eventlogger.events.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
class LoggerEventSummaryTest {
    @Autowired
    private LoggerEventSummary loggerEventSummary;

    @Autowired
    private SqlEventLogger logger;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Test
    void getSummaryBasic() {
        String jobId = "Job1";
        String usr = "USER";
        OffsetDateTime firstTime = OffsetDateTime.now().minusDays(11);
        File file1 = new File("./file1");

        LoggableEvent e1 = new JobStatusChangeEvent(usr, jobId, null, "SUBMITTED", "Job Created");
        e1.setTimeOfEvent(firstTime);
        logger.log(e1);
        LoggableEvent e2 = new JobStatusChangeEvent(usr, jobId, "SUBMITTED", "IN_PROGRESS", "Job Started");
        e2.setTimeOfEvent(firstTime.plusDays(1));
        logger.log(e2);
        LoggableEvent e3 = new FileEvent(usr, jobId, file1, FileEvent.FileStatus.OPEN);
        e3.setTimeOfEvent(firstTime.plusDays(2));
        logger.log(e3);
        LoggableEvent e4 = new FileEvent(usr, jobId, file1, FileEvent.FileStatus.OPEN);
        e4.setTimeOfEvent(firstTime.plusDays(3));
        logger.log(e4);
        LoggableEvent e5 = new ContractSearchEvent(usr, jobId, "CONTRACT1", 100, 90, 80, 2, 70, 1000, 2000, 1);
        e5.setTimeOfEvent(firstTime.plusDays(4));
        logger.log(e5);
        LoggableEvent e6 = new FileEvent(usr, jobId, file1, FileEvent.FileStatus.CLOSE);
        e6.setTimeOfEvent(firstTime.plusDays(5));
        logger.log(e6);
        LoggableEvent e7 = new ApiRequestEvent(usr, jobId, "/file", "1.1.1.1", "abc", "request1");
        e7.setTimeOfEvent(firstTime.plusDays(6));
        logger.log(e7);
        LoggableEvent e8 = new JobStatusChangeEvent(usr, jobId, "IN_PROGRESS", "SUCCESSFUL", "Job Done");
        e8.setTimeOfEvent(firstTime.plusDays(7));
        logger.log(e8);
        LoggableEvent e9 = new ApiResponseEvent(usr, jobId, HttpStatus.OK, "File Download", "", "request1");
        e9.setTimeOfEvent(firstTime.plusDays(8));
        logger.log(e9);
        LoggableEvent e10 = new FileEvent(usr, jobId, file1, FileEvent.FileStatus.DELETE);
        e10.setTimeOfEvent(firstTime.plusDays(9));
        logger.log(e10);

        JobSummaryEvent summary = loggerEventSummary.getSummary(jobId);
        assertEquals(jobId, summary.getJobId());
        assertEquals(usr, summary.getOrganization());
        assertEquals(firstTime.getNano(), summary.getSubmittedTime().getNano());
        assertEquals(firstTime.plusDays(1).getNano(), summary.getInProgressTime().getNano());
        assertEquals(firstTime.plusDays(7).getNano(), summary.getSuccessfulTime().getNano());
        assertNull(summary.getCancelledTime());
        assertNull(summary.getFailedTime());
        assertEquals(1, summary.getNumFilesCreated());
        assertEquals(1, summary.getNumFilesDeleted());
        assertEquals(1, summary.getNumFilesDownloaded());
        assertEquals(100, summary.getTotalNum());
        assertEquals(80, summary.getSuccessfullySearched());
        assertEquals(2, summary.getErrorSearched());
    }

    @Test
    void getSummaryFailed() {
        OffsetDateTime firstTime = OffsetDateTime.now().minusDays(11);
        JobSummaryEvent summary = getSummaryError("JOBFAIL", "FAILED", firstTime);
        assertEquals(firstTime.plusDays(7).getNano(), summary.getFailedTime().getNano());
        assertNull(summary.getCancelledTime());
    }

    @Test
    void getSummaryCancelled() {
        OffsetDateTime firstTime = OffsetDateTime.now().minusDays(11);
        JobSummaryEvent summary = getSummaryError("JOBCANCEL", "CANCELLED", firstTime);
        assertEquals(firstTime.plusDays(7).getNano(), summary.getCancelledTime().getNano());
        assertNull(summary.getFailedTime());
    }

    JobSummaryEvent getSummaryError(String jobId, String state, OffsetDateTime firstTime) {
        String usr = "USER";
        File file1 = new File("./file1");

        LoggableEvent e1 = new JobStatusChangeEvent(usr, jobId, null, "SUBMITTED", "Job Created");
        e1.setTimeOfEvent(firstTime);
        logger.log(e1);
        LoggableEvent e2 = new JobStatusChangeEvent(usr, jobId, "SUBMITTED", "IN_PROGRESS", "Job Started");
        e2.setTimeOfEvent(firstTime.plusDays(1));
        logger.log(e2);
        LoggableEvent e3 = new FileEvent(usr, jobId, file1, FileEvent.FileStatus.OPEN);
        e3.setTimeOfEvent(firstTime.plusDays(2));
        logger.log(e3);
        LoggableEvent e4 = new FileEvent(usr, jobId, file1, FileEvent.FileStatus.OPEN);
        e4.setTimeOfEvent(firstTime.plusDays(3));
        logger.log(e4);
        LoggableEvent e6 = new FileEvent(usr, jobId, file1, FileEvent.FileStatus.CLOSE);
        e6.setTimeOfEvent(firstTime.plusDays(5));
        logger.log(e6);
        LoggableEvent e8 = new JobStatusChangeEvent(usr, jobId, "IN_PROGRESS", state, "Job Done");
        e8.setTimeOfEvent(firstTime.plusDays(7));
        logger.log(e8);
        LoggableEvent e10 = new FileEvent(usr, jobId, file1, FileEvent.FileStatus.DELETE);
        e10.setTimeOfEvent(firstTime.plusDays(9));
        logger.log(e10);

        JobSummaryEvent summary = loggerEventSummary.getSummary(jobId);
        assertEquals(jobId, summary.getJobId());
        assertEquals(usr, summary.getOrganization());
        assertEquals(firstTime.getNano(), summary.getSubmittedTime().getNano());
        assertEquals(firstTime.plusDays(1).getNano(), summary.getInProgressTime().getNano());
        assertNull(summary.getSuccessfulTime());
        assertEquals(1, summary.getNumFilesCreated());
        assertEquals(1, summary.getNumFilesDeleted());
        assertEquals(0, summary.getNumFilesDownloaded());
        assertEquals(0, summary.getTotalNum());
        assertEquals(0, summary.getSuccessfullySearched());
        assertEquals(0, summary.getNumOptedOut());
        assertEquals(0, summary.getErrorSearched());
        return summary;
    }

    @Test
    void getUniqueNumFilesOfType() {
        List<LoggableEvent> fileEvents = new ArrayList<>();
        assertEquals(0, loggerEventSummary.getUniqueNumFilesOfType(fileEvents, FileEvent.FileStatus.OPEN));
        FileEvent event1 = createFileEvent("Job1", "file1", FileEvent.FileStatus.OPEN, "hash1");
        fileEvents.add(event1);
        assertEquals(1, loggerEventSummary.getUniqueNumFilesOfType(fileEvents, FileEvent.FileStatus.OPEN));
        FileEvent event2 = createFileEvent("Job1", "file1", FileEvent.FileStatus.OPEN, "hash1");
        fileEvents.add(event2);
        assertEquals(2, loggerEventSummary.getUniqueNumFilesOfType(fileEvents, FileEvent.FileStatus.OPEN));
        FileEvent event3 = createFileEvent("Job1", "file1", FileEvent.FileStatus.CLOSE, "hash1");
        assertEquals(0, loggerEventSummary.getUniqueNumFilesOfType(fileEvents, FileEvent.FileStatus.CLOSE));
        fileEvents.add(event3);
        assertEquals(1, loggerEventSummary.getUniqueNumFilesOfType(fileEvents, FileEvent.FileStatus.CLOSE));
        FileEvent event4 = createFileEvent("Job1", "file1", FileEvent.FileStatus.DELETE, "hash1");
        assertEquals(0, loggerEventSummary.getUniqueNumFilesOfType(fileEvents, FileEvent.FileStatus.DELETE));
        fileEvents.add(event4);
        assertEquals(1, loggerEventSummary.getUniqueNumFilesOfType(fileEvents, FileEvent.FileStatus.DELETE));

        // Finally, make sure even after additions, these still stay the same
        assertEquals(2, loggerEventSummary.getUniqueNumFilesOfType(fileEvents, FileEvent.FileStatus.OPEN));
        assertEquals(1, loggerEventSummary.getUniqueNumFilesOfType(fileEvents, FileEvent.FileStatus.CLOSE));
    }

    FileEvent createFileEvent(String jobId, String fileName, FileEvent.FileStatus status, String hash) {
        FileEvent fileEvent = new FileEvent();
        fileEvent.setJobId(jobId);
        fileEvent.setFileName(fileName);
        fileEvent.setFileHash(hash);
        fileEvent.setStatus(status);
        return fileEvent;
    }

    @Test
    void getTime() {
        OffsetDateTime now = OffsetDateTime.now();
        assertNull(loggerEventSummary.getTime(null, "SUBMITTED"));
        List<LoggableEvent> events = new ArrayList<>();
        assertNull(loggerEventSummary.getTime(null, "SUBMITTED"));
        OffsetDateTime createTime = now.minusDays(5);
        OffsetDateTime inProgressTime = now.minusDays(4);
        // OffsetDateTime cancelledTime = now.minusDays(3);
        OffsetDateTime successTime = now.minusDays(2);
        // OffsetDateTime failedTime = now.minusDays(1);
        events.add(createEvent(createTime, "SUBMITTED"));
        assertNull(loggerEventSummary.getTime(null, "FAILED"));
        events.add(createEvent(now, "SUBMITTED"));
        events.add(createEvent(inProgressTime, "IN_PROGRESS"));
        events.add(createEvent(successTime, "SUCCESSFUL"));
        assertEquals(loggerEventSummary.getTime(events, "SUBMITTED"), createTime);
        assertEquals(loggerEventSummary.getTime(events, "IN_PROGRESS"), inProgressTime);
        assertEquals(loggerEventSummary.getTime(events, "SUCCESSFUL"), successTime);
    }

    JobStatusChangeEvent createEvent(OffsetDateTime timeOfEvent, String status) {
        JobStatusChangeEvent event = new JobStatusChangeEvent();
        event.setTimeOfEvent(timeOfEvent);
        event.setOrganization("USER");
        event.setJobId("JOB1");
        event.setNewStatus(status);
        return event;
    }
}