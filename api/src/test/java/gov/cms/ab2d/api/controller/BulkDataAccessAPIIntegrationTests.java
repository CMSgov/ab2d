package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.repository.JobRepository;
import gov.cms.ab2d.domain.Job;
import gov.cms.ab2d.domain.JobStatus;
import org.hamcrest.core.Is;
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
import org.springframework.test.web.servlet.ResultActions;

import static gov.cms.ab2d.api.service.JobService.INITIAL_JOB_STATUS_MESSAGE;
import static gov.cms.ab2d.api.util.Constants.API_PREFIX;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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

        String statusUrl = "http://localhost" + API_PREFIX + "/status/" + job.getJobID();

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string("Content-Location", statusUrl))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("information")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("informational")))
                .andExpect(jsonPath("$.issue[0].details.text", Is.is("Request " + job.getJobID() + " accepted for processing")));

        Assert.assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        Assert.assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        Assert.assertEquals(job.getProgress(), null);
        Assert.assertEquals(job.getRequestURL(), "http://localhost" + API_PREFIX  + PATIENT_EXPORT_PATH);
        Assert.assertEquals(job.getResourceTypes(), null);
        Assert.assertEquals(job.getUser(), null);
    }

    @Test
    public void testPatientExportWithParameters() throws Exception {
        final String typeParams = "?_type=Patient,ExplanationOfBenefits&_outputFormat=application/fhir+ndjson&since=20191015";
        ResultActions resultActions = this.mockMvc.perform(get(API_PREFIX + "/" + PATIENT_EXPORT_PATH + typeParams)
                .contentType(MediaType.APPLICATION_JSON)).andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl = "http://localhost" + API_PREFIX + "/status/" + job.getJobID();

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string("Content-Location", statusUrl))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("information")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("informational")))
                .andExpect(jsonPath("$.issue[0].details.text", Is.is("Request " + job.getJobID() + " accepted for processing")));

        Assert.assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        Assert.assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        Assert.assertEquals(job.getProgress(), null);
        Assert.assertEquals(job.getRequestURL(), "http://localhost" + API_PREFIX + PATIENT_EXPORT_PATH + typeParams);
        Assert.assertEquals(job.getResourceTypes(), "Patient,ExplanationOfBenefits");
        Assert.assertEquals(job.getUser(), null);
    }

    @Test
    public void testPatientExportWithInvalidParameters() throws Exception {
        final String typeParams = "?_type=PatientInvalid,ExplanationOfBenefits";
        this.mockMvc.perform(get(API_PREFIX + "/" + PATIENT_EXPORT_PATH + typeParams)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400));
    }
}