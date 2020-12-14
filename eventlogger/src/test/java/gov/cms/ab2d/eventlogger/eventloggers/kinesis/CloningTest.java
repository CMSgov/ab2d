package gov.cms.ab2d.eventlogger.eventloggers.kinesis;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.*;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class CloningTest {
    @Test
    void testCloneApiRequest() {
        ApiRequestEvent event = new ApiRequestEvent("user", "jobId", "url", "ipAddress", "token", "requestId");
        setSuperClassVariables(event);
        ApiRequestEvent clone = event.clone();
        assertEquals(clone.getUrl(), "url");
        assertEquals(clone.getIpAddress(), "ipAddress");
        assertEquals(clone.getTokenHash(), UtilMethods.hashIt("token"));
        assertEquals(clone.getRequestId(), "requestId");
        testSuperClass(event, clone);
    }

    @Test
    void testCloneApiResponse() {
        ApiResponseEvent event = new ApiResponseEvent("user", "jobId", HttpStatus.ACCEPTED,
                "response", "description", "requestId");
        setSuperClassVariables(event);
        ApiResponseEvent clone = event.clone();
        assertEquals(clone.getResponseCode(), HttpStatus.ACCEPTED.value());
        assertEquals(clone.getResponseString(), "response");
        assertEquals(clone.getDescription(), "description");
        assertEquals(clone.getRequestId(), "requestId");
        testSuperClass(event, clone);
    }

    @Test
    void testCloneBeneSearchEvent() {
        OffsetDateTime dt = OffsetDateTime.now();
        OffsetDateTime end = OffsetDateTime.now().plusDays(1);
        BeneficiarySearchEvent event = new BeneficiarySearchEvent("user", "jobId", "contractNum", dt, end,
                "beneId", "response");
        setSuperClassVariables(event);
        BeneficiarySearchEvent clone = event.clone();
        assertEquals(clone.getResponse(), "response");
        assertEquals(clone.getTimeOfEvent(), dt);
        assertEquals(clone.getResponseDate(), end);
        assertEquals(clone.getBeneId(), "beneId");
        assertEquals(clone.getContractNum(), "contractNum");
        testSuperClass(event, clone);
    }

    @Test
    void testCloneContractBeneSearchEvent() {
        ContractBeneSearchEvent event = new ContractBeneSearchEvent("user", "jobId",
                "contractNumber", 100, 98, 2);
        setSuperClassVariables(event);
        ContractBeneSearchEvent clone = event.clone();
        assertEquals(clone.getContractNumber(), "contractNumber");
        assertEquals(clone.getNumInContract(), 100);
        assertEquals(clone.getNumSearched(), 98);
        assertEquals(clone.getNumErrors(), 2);
        testSuperClass(event, clone);

    }

    @Test
    void testCloneErrorEvent() {
        ErrorEvent event = new ErrorEvent("user", "jobId", ErrorEvent.ErrorType.CONTRACT_NOT_FOUND, "description");
        setSuperClassVariables(event);
        ErrorEvent clone = event.clone();
        assertEquals(clone.getErrorType(), ErrorEvent.ErrorType.CONTRACT_NOT_FOUND);
        assertEquals(clone.getDescription(), "description");
        testSuperClass(event, clone);
    }

    @Test
    void testCloneFileEvent() {
        File f = new File("/tmp");
        long length = f.length();
        String hash = "";
        try (FileInputStream fileInputStream = new FileInputStream(f)) {
            hash = UtilMethods.hashIt(fileInputStream);
        } catch (IOException e) {
            fail();
        }
        FileEvent event = new FileEvent("user", "jobId", f, FileEvent.FileStatus.OPEN);
        setSuperClassVariables(event);
        FileEvent clone = event.clone();
        assertEquals(clone.getFileName(), "/tmp");
        assertEquals(clone.getStatus(), FileEvent.FileStatus.OPEN);
        assertEquals(clone.getFileSize(), length);
        assertEquals(clone.getFileHash(), hash);
        testSuperClass(event, clone);
    }

    @Test
    void testCloneJobStatusChangeEvent() {
        JobStatusChangeEvent event = new JobStatusChangeEvent("user", "jobId", "oldStatus",
                "newStatus", "description");
        setSuperClassVariables(event);
        JobStatusChangeEvent clone = event.clone();
        assertEquals(clone.getOldStatus(), "oldStatus");
        assertEquals(clone.getNewStatus(), "newStatus");
        assertEquals(clone.getDescription(), "description");
        testSuperClass(event, clone);

        LoggableEvent newLogEvent = event.clone();
        if (event.getUser() != null && !event.getUser().isEmpty()) {
            newLogEvent.setUser(DigestUtils.md5Hex(event.getUser()).toUpperCase());
        }
        assertEquals(((JobStatusChangeEvent) newLogEvent).getOldStatus(), "oldStatus");
        assertEquals(((JobStatusChangeEvent) newLogEvent).getNewStatus(), "newStatus");
        assertEquals(((JobStatusChangeEvent) newLogEvent).getDescription(), "description");
        assertEquals(newLogEvent.getEnvironment(), "environment");
        assertEquals(newLogEvent.getId(), 1L);
        assertEquals(newLogEvent.getAwsId(), "awsId");
        assertEquals(newLogEvent.getJobId(), "jobId");
        assertEquals(newLogEvent.getUser(), DigestUtils.md5Hex("user").toUpperCase());
        assertEquals(newLogEvent.getTimeOfEvent(), event.getTimeOfEvent());
        assertNotNull(newLogEvent.getTimeOfEvent());
    }

    @Test
    void testCloneJobSummaryEvent() {
        JobSummaryEvent event = new JobSummaryEvent();
        setSuperClassVariables(event);
        OffsetDateTime d0 = OffsetDateTime.now();
        event.setTimeOfEvent(d0);
        OffsetDateTime d1 = OffsetDateTime.now();
        OffsetDateTime d2 = d1.plusDays(1);
        OffsetDateTime d3 = d2.plusDays(1);
        OffsetDateTime d4 = d3.plusDays(1);
        OffsetDateTime d5 = d4.plusDays(1);
        event.setSubmittedTime(d1);
        event.setInProgressTime(d2);
        event.setSuccessfulTime(d3);
        event.setCancelledTime(d4);
        event.setFailedTime(d5);
        event.setNumFilesCreated(1);
        event.setNumFilesDeleted(2);
        event.setNumFilesDownloaded(3);
        event.setTotalNum(4);
        event.setSuccessfullySearched(5);
        event.setNumOptedOut(6);
        event.setErrorSearched(7);
        JobSummaryEvent clone = event.clone();
        assertEquals(clone.getSubmittedTime(), d1);
        assertEquals(clone.getInProgressTime(), d2);
        assertEquals(clone.getSuccessfulTime(), d3);
        assertEquals(clone.getCancelledTime(), d4);
        assertEquals(clone.getFailedTime(), d5);
        assertEquals(clone.getNumFilesCreated(), 1);
        assertEquals(clone.getNumFilesDeleted(), 2);
        assertEquals(clone.getNumFilesDownloaded(), 3);
        assertEquals(clone.getTotalNum(), 4);
        assertEquals(clone.getSuccessfullySearched(), 5);
        assertEquals(clone.getNumOptedOut(), 6);
        assertEquals(clone.getErrorSearched(), 7);
        testSuperClass(event, clone);
    }

    @Test
    void testCloneReloadEvent() {
        ReloadEvent event = new ReloadEvent("user", ReloadEvent.FileType.ATTESTATION_REPORT, "fileName", 100);
        setSuperClassVariables(event);
        ReloadEvent clone = event.clone();
        assertEquals(clone.getFileType(), ReloadEvent.FileType.ATTESTATION_REPORT);
        assertEquals(clone.getFileName(), "fileName");
        assertEquals(clone.getNumberLoaded(), 100);
        testSuperClass(event, clone);
    }

    void setSuperClassVariables(LoggableEvent event) {
        event.setUser("user");
        event.setAwsId("awsId");
        event.setEnvironment("environment");
        event.setJobId("jobId");
        event.setId(1L);
    }

    void testSuperClass(LoggableEvent event, LoggableEvent clone) {
        assertEquals(clone.getEnvironment(), "environment");
        assertEquals(clone.getId(), 1L);
        assertEquals(clone.getAwsId(), "awsId");
        assertEquals(clone.getJobId(), "jobId");
        assertEquals(clone.getUser(), "user");
        assertEquals(clone.getTimeOfEvent(), event.getTimeOfEvent());
        assertNotNull(clone.getTimeOfEvent());
    }
}
