package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.controller.common.ApiCommon;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.job.service.JobOutputMissingException;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ErrorHandlerTest {

    ErrorHandler handler;
    MockHttpServletRequest request;
    MockHttpServletResponse response;

    @BeforeEach
    void setup() {
        handler = new ErrorHandler(
            mock(SQSEventClient.class),
            -1,
            mock(ApiCommon.class)
        );
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void test_download_job_not_found() throws Exception {
        request.setRequestURI("https://ab2d.cms.gov/api/v2/fhir/Job/1234/file/Z0001.json");
        handler.generateFHIRError(new ResourceNotFoundException("No job with jobUuid 1234 was found"), request, response);

        assertEquals(404, response.getStatus());
        assertJsonEquals(
        """
        {
          "resourceType": "OperationOutcome",
          "issue": [
            {
              "severity": "error",
              "code": "invalid",
              "details": {
                "text": "No job with jobUuid 1234 was found"
              }
            }
          ]
        }
        """,
        response.getContentAsString());
    }

    @Test
    void test_download_file_expired() throws Exception {
        request.setRequestURI("https://ab2d.cms.gov/api/v2/fhir/Job/1234/file/Z0001.json");
        handler.generateFHIRError(new JobOutputMissingException("The file is not present as it has expired. Please resubmit the job"), request, response);

        assertEquals(500, response.getStatus());
        assertJsonEquals(
        """
        {
          "resourceType": "OperationOutcome",
          "issue": [
            {
              "severity": "error",
              "code": "invalid",
              "details": {
                "text": "The file is not present as it has expired. Please resubmit the job"
              }
            }
          ]
        }
        """,
        response.getContentAsString());
    }

    /**
     * Exception messages that don't originate from AB2D code should not be sent to user
     * See {@link ErrorHandler#getRootCause)} for details
     */
    @Test
    void test_download_exception_thrown_outside_ab2d() throws Exception {
        request.setRequestURI("https://ab2d.cms.gov/api/v2/fhir/Job/1234/file/Z0001.json");
        handler.generateFHIRError(new PSQLException("oops", PSQLState.UNKNOWN_STATE), request, response);

        assertEquals(500, response.getStatus());
        assertJsonEquals(
        """
        {
          "resourceType": "OperationOutcome",
          "issue": [
            {
              "severity": "error",
              "code": "invalid",
              "details": {
                "text": "An internal error occurred"
              }
            }
          ]
        }
        """,
        response.getContentAsString());
    }

    private void assertJsonEquals(String expected, String actual) throws Exception {
        val mapper = new ObjectMapper();
        assertEquals(mapper.readTree(expected), mapper.readTree(actual));
    }
}
