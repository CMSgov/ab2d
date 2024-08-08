package gov.cms.ab2d.api.controller.common;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
  void testDoStatus1() {
    when(jobPollResult.getStatus()).thenReturn(JobStatus.SUCCESSFUL);
    assertNotNull(
      statusCommon.doStatus("1234", req, "prefix")
    );
  }

  @Test
  void testDoStatus2() {
    when(jobPollResult.getStatus()).thenReturn(JobStatus.SUBMITTED);
    assertNotNull(
      statusCommon.doStatus("1234", req, "prefix")
    );
  }

  @Test
  void testDoStatus3() {
    when(jobPollResult.getStatus()).thenReturn(JobStatus.IN_PROGRESS);
    assertNotNull(
      statusCommon.doStatus("1234", req, "prefix")
    );
  }

  @Test
  void testDoStatus4() {
    when(jobPollResult.getStatus()).thenReturn(JobStatus.FAILED);
    assertThrows(JobProcessingException.class, () -> {
      statusCommon.doStatus("1234", req, "prefix");
    });
  }

  @Test
  void testDoStatus5() {
    when(jobPollResult.getStatus()).thenReturn(JobStatus.CANCELLED);
    assertThrows(JobProcessingException.class, () -> {
      statusCommon.doStatus("1234", req, "prefix");
    });
  }

  @Test
  void testCancelJob() {
    assertNotNull(
      statusCommon.cancelJob("1234", req)
    );
  }

}
