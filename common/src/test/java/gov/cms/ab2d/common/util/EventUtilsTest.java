package gov.cms.ab2d.common.util;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.eventlogger.events.JobStatusChangeEvent;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static gov.cms.ab2d.common.model.JobStatus.IN_PROGRESS;
import static gov.cms.ab2d.common.model.JobStatus.SUCCESSFUL;
import static gov.cms.ab2d.fhir.Versions.FhirVersions.STU3;
import static org.junit.jupiter.api.Assertions.*;

class EventUtilsTest {
    private Job job;
    private String jobId = "ABC";
    private User user;
    private String userName = "DEF";

    @BeforeEach
    void init() {
        user = new User();
        user.setUsername(userName);
        job = new Job();
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setJobUuid(jobId);
        job.setUser(user);
        job.setFhirVersion(STU3);
    }

    @Test
    void getJobChangeEvent() {
        JobStatusChangeEvent event = EventUtils.getJobChangeEvent(
                null, null, null);
        assertNull(event.getDescription());
        assertNull(event.getOldStatus());
        assertNull(event.getNewStatus());
        assertNull(event.getJobId());
        assertNotNull(event.getTimeOfEvent());
        assertNull(event.getUser());

        event = EventUtils.getJobChangeEvent(job, SUCCESSFUL, "Hello World");
        assertEquals(event.getOldStatus(), IN_PROGRESS.name());
        assertEquals(SUCCESSFUL.name(), event.getNewStatus());
        assertEquals(jobId, event.getJobId());
        assertEquals(userName, event.getUser());
        assertEquals("Hello World", event.getDescription());
    }

    @Test
    void getFileEvent() throws IOException {
        FileEvent event = EventUtils.getFileEvent(null, null, null);
        assertNull(event.getFileHash());
        assertNull(event.getFileName());
        assertEquals(0, event.getFileSize());
        assertNull(event.getStatus());
        assertNull(event.getJobId());
        assertNotNull(event.getTimeOfEvent());

        File f = null;
        try {
            f = File.createTempFile("abcd", "");
            f.createNewFile();
            FileUtils.writeStringToFile(f, "Hello World");

            event = EventUtils.getFileEvent(job, f, FileEvent.FileStatus.CLOSE);
            assertNotNull(event.getFileHash());
            assertTrue(event.getFileName().contains("abcd"));
            assertEquals(11, event.getFileSize());
            assertEquals(FileEvent.FileStatus.CLOSE, event.getStatus());
            assertEquals(jobId, event.getJobId());
            assertNotNull(event.getTimeOfEvent());
        } finally {
            if (f != null) {
                f.delete();
            }
        }
    }
}