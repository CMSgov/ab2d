package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.repository.JobRepository;
import gov.cms.ab2d.api.util.DateUtil;
import gov.cms.ab2d.domain.Job;
import gov.cms.ab2d.domain.JobOutput;
import gov.cms.ab2d.domain.JobStatus;
import org.hamcrest.core.Is;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Date;

import static gov.cms.ab2d.api.service.JobServiceImpl.INITIAL_JOB_STATUS_MESSAGE;
import static gov.cms.ab2d.api.util.Constants.API_PREFIX;
import static gov.cms.ab2d.api.util.Constants.OPERATION_OUTCOME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class BulkDataAccessAPIIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    private static final String PATIENT_EXPORT_PATH = "/Patient/$export";

    @Test
    public void testBasicPatientExport() throws Exception {
        ResultActions resultActions = this.mockMvc.perform(get(API_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl = "http://localhost" + API_PREFIX + "/Job/" + job.getJobID() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string("Content-Location", statusUrl));

        Assert.assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        Assert.assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        Assert.assertEquals(job.getProgress(), Integer.valueOf(0));
        Assert.assertEquals(job.getRequestURL(), "http://localhost" + API_PREFIX  + PATIENT_EXPORT_PATH);
        Assert.assertEquals(job.getResourceTypes(), null);
        Assert.assertEquals(job.getUser(), null);
    }

    @Test
    public void testPatientExportWithParameters() throws Exception {
        final String typeParams = "?_type=ExplanationOfBenefits&_outputFormat=application/fhir+ndjson&since=20191015";
        ResultActions resultActions = this.mockMvc.perform(get(API_PREFIX + "/" + PATIENT_EXPORT_PATH + typeParams)
                .contentType(MediaType.APPLICATION_JSON)).andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl = "http://localhost" + API_PREFIX + "/Job/" + job.getJobID() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string("Content-Location", statusUrl));

        Assert.assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        Assert.assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        Assert.assertEquals(job.getProgress(), Integer.valueOf(0));
        Assert.assertEquals(job.getRequestURL(), "http://localhost" + API_PREFIX + PATIENT_EXPORT_PATH + typeParams);
        Assert.assertEquals(job.getResourceTypes(), "ExplanationOfBenefits");
        Assert.assertEquals(job.getUser(), null);
    }

    @Test
    public void testPatientExportWithInvalidType() throws Exception {
        final String typeParams = "?_type=PatientInvalid,ExplanationOfBenefits";
        this.mockMvc.perform(get(API_PREFIX + "/" + PATIENT_EXPORT_PATH + typeParams)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text", Is.is("IllegalArgumentException: _type must be ExplanationOfBenefits")));
    }

    @Test
    public void testPatientExportWithInvalidOutputFormat() throws Exception {
        final String typeParams = "?_outputFormat=Invalid";
        this.mockMvc.perform(get(API_PREFIX + "/" + PATIENT_EXPORT_PATH + typeParams)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text", Is.is("IllegalArgumentException: An _outputFormat of Invalid is not valid")));
    }

    @Test
    public void testGetStatusWhileInProgress() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get(API_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        String statusUrl = mvcResult.getResponse().getHeader("Content-Location");

        this.mockMvc.perform(get(statusUrl).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is(202))
            .andExpect(header().string("X-Progress", "0% complete"))
            .andExpect(header().string("Retry-After", "30"));

        // Immediate repeat of status check should produce 429.
        this.mockMvc.perform(get(statusUrl)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(429))
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(header().doesNotExist("X-Progress"));
    }

    @Test
    public void testGetStatusWhileFinished() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get(API_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefits").contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String statusUrl = mvcResult.getResponse().getHeader("Content-Location");

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.SUCCESSFUL);
        job.setProgress(100);
        LocalDateTime expireDate = LocalDateTime.now().plusDays(100);
        job.setExpires(expireDate);
        LocalDateTime now = LocalDateTime.now();
        job.setCompletedAt(now);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setFhirResourceType("ExplanationOfBenefits");
        jobOutput.setJob(job);
        jobOutput.setFilePath("http://localhost/some/path/file.ndjson");
        job.getJobOutput().add(jobOutput);

        JobOutput errorJobOutput = new JobOutput();
        errorJobOutput.setFhirResourceType(OPERATION_OUTCOME);
        errorJobOutput.setJob(job);
        errorJobOutput.setFilePath("http://localhost/some/path/error.ndjson");
        errorJobOutput.setError(true);
        job.getJobOutput().add(errorJobOutput);

        jobRepository.saveAndFlush(job);

        this.mockMvc.perform(get(statusUrl).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andExpect(header().string("Expires", DateUtil.formatLocalDateTimeAsUTC(expireDate)))
                .andExpect(jsonPath("$.transactionTime", Is.is(new DateTimeType(DateUtil.convertLocalDateTimeToDate(now)).toHumanDisplay())))
                .andExpect(jsonPath("$.request", Is.is(job.getRequestURL())))
                .andExpect(jsonPath("$.requiresAccessToken", Is.is(true)))
                .andExpect(jsonPath("$.output[0].type", Is.is("ExplanationOfBenefits")))
                .andExpect(jsonPath("$.output[0].url", Is.is("http://localhost/some/path/file.ndjson")))
                .andExpect(jsonPath("$.error[0].type", Is.is(OPERATION_OUTCOME)))
                .andExpect(jsonPath("$.error[0].url", Is.is("http://localhost/some/path/error.ndjson")));
    }

    @Test
    public void testGetStatusWhileFailed() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get(API_PREFIX + PATIENT_EXPORT_PATH + "?_type=ExplanationOfBenefits").contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String statusUrl = mvcResult.getResponse().getHeader("Content-Location");

        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.FAILED);
        LocalDateTime expireDate = LocalDateTime.now().plusDays(100);
        job.setExpires(expireDate);

        jobRepository.saveAndFlush(job);

        this.mockMvc.perform(get(statusUrl).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text", Is.is("JobProcessingException: Job failed while processing")));
    }
}