package gov.cms.ab2d.api.controller.common;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import gov.cms.ab2d.api.controller.JobProcessingException;
import gov.cms.ab2d.api.controller.TooManyRequestsException;
import gov.cms.ab2d.api.remote.JobClient;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.model.TooFrequentInvocations;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.job.dto.JobPollResult;
import gov.cms.ab2d.job.model.JobStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class StatusCommonTest {

  StatusCommon statusCommon;
  PdpClientService pdpClientService;
  PdpClient pdpClient;
  JobClient jobClient;
  JobPollResult jobPollResult;
  SQSEventClient eventLogger;
  MockHttpServletRequest req;

  @BeforeEach
  void beforeEach() {
    pdpClientService = mock(PdpClientService.class);
    pdpClient = mock(PdpClient.class);
    when(pdpClientService.getCurrentClient()).thenReturn(pdpClient);

    jobClient = mock(JobClient.class);
    jobPollResult = mock(JobPollResult.class);
    when(jobClient.poll(anyBoolean(), any(), any(), anyInt())).thenReturn(jobPollResult);
    when(jobPollResult.getExpiresAt()).thenReturn(OffsetDateTime.now());

    eventLogger = mock(SQSEventClient.class);
    statusCommon = new StatusCommon(pdpClientService, jobClient, eventLogger, 0);

    req = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    req.setScheme("http");
    req.setServerName("localhost");
    req.setServerPort(8080);
  }

  @Test
  void testThrowFailedResponse() {
    assertThrows(JobProcessingException.class, () -> {
      statusCommon.throwFailedResponse("error!");
    });
  }

  @Test
  void testTooFrequentInvocations() {
    when(jobClient.poll(anyBoolean(), any(), any(), anyInt())).thenThrow(TooFrequentInvocations.class);
    assertThrows(TooManyRequestsException.class, () -> {
      statusCommon.doStatus("1234", req, "prefix");
    });
  }

  @Test
  void testDoStatusSuccessful() {
    when(jobPollResult.getStatus()).thenReturn(JobStatus.SUCCESSFUL);
    assertNotNull(
      statusCommon.doStatus("1234", req, "prefix")
    );
  }

  @Test
  void testDoStatusSubmitted() {
    when(jobPollResult.getStatus()).thenReturn(JobStatus.SUBMITTED);
    assertNotNull(
      statusCommon.doStatus("1234", req, "prefix")
    );
  }

  @Test
  void testDoStatusInProgress() {
    when(jobPollResult.getStatus()).thenReturn(JobStatus.IN_PROGRESS);
    assertNotNull(
      statusCommon.doStatus("1234", req, "prefix")
    );
  }

  @Test
  void testDoStatusFailed() {
    when(jobPollResult.getStatus()).thenReturn(JobStatus.FAILED);
    assertThrows(JobProcessingException.class, () -> {
      statusCommon.doStatus("1234", req, "prefix");
    });
  }

  @Test
  void testDoStatusCanceled() {
    when(jobPollResult.getStatus()).thenReturn(JobStatus.CANCELLED);
    assertNotNull(
      statusCommon.doStatus("1234", req, "prefix")
    );
  }

  @Test
  void testCancelJob() {
    assertNotNull(
      statusCommon.cancelJob("1234", req)
    );
  }

  @Test
  void testGetJobCompletedResponse() {
    assertNotNull(
      statusCommon.getJobCompletedResponse(jobPollResult, "1234", req, "prefix")
    );
  }

  @Test
  void testGetJobCanceledResponse() {
    assertNotNull(
      statusCommon.getCanceledResponse(jobPollResult, "1234", req)
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {
        "Z0000_0001.ndjson.gz",
        "Z0000_0001.ndjson"
  })
  void testGetUrlPathData(String file) {
    assertEquals(
            "http://localhost:8080/v1/fhir/Job/1234/file/Z0000_0001.ndjson",
            statusCommon.getUrlPath("1234", file, req, "v1")
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {
          "Z0000_0001_error.ndjson.gz",
          "Z0000_0001_error.ndjson"
  })
  void testGetUrlPathError(String file) {
    assertEquals(
            "http://localhost:8080/v1/fhir/Job/1234/file/Z0000_0001_error.ndjson",
            statusCommon.getUrlPath("1234", file, req, "v1")
    );
  }



}
