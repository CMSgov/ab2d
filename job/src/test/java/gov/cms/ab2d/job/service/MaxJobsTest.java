package gov.cms.ab2d.job.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.job.JobTestSpringBootApp;
import gov.cms.ab2d.job.dto.StartJobDTO;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.job.repository.JobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.job.service.JobServiceTest.CLIENTID;
import static gov.cms.ab2d.job.service.JobServiceTest.CONTRACT_NUMBER;
import static gov.cms.ab2d.job.service.JobServiceTest.LOCAL_HOST;
import static gov.cms.ab2d.job.service.JobServiceTest.setupRegularClientSecurityContext;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = JobTestSpringBootApp.class)
@Testcontainers
class MaxJobsTest extends JobCleanup {

    private static final int MAX_JOBS_PER_CLIENT = 3;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private PdpClientService pdpClientService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private DataSetup dataSetup;

    private StartJobDTO startJobDTO;

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @BeforeEach
    public void setup() {
        createMaxJobs();
    }

    @AfterEach
    public void cleanup() {
        jobCleanup();
        dataSetup.cleanup();
    }

    private void createMaxJobs() {
        dataSetup.setupNonStandardClient(CLIENTID, CONTRACT_NUMBER, of());
        setupRegularClientSecurityContext();


        Contract contract = contractRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        String organization = pdpClientService.getCurrentClient().getOrganization();
        startJobDTO = new StartJobDTO(contract.getContractNumber(), organization,
                EOB, LOCAL_HOST, NDJSON_FIRE_CONTENT_TYPE, null, STU3);
        for (int idx = 0; idx < MAX_JOBS_PER_CLIENT; idx++) {
            Job retJob = jobService.createJob(startJobDTO);
            assertNotNull(retJob);
            addJobForCleanup(retJob);
        }
    }

    @Test
    void testPatientExportDuplicateSubmissionWithCancelledStatus() {
        int maxJobs = pdpClientService.getCurrentClient().getMaxParallelJobs();
        assertEquals(MAX_JOBS_PER_CLIENT, maxJobs);
        assertEquals(MAX_JOBS_PER_CLIENT, jobService.activeJobs(startJobDTO.getOrganization()));

        List<Job> jobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        for (Job job : jobs) {
            job.setStatus(JobStatus.CANCELLED);
            jobRepository.saveAndFlush(job);
        }

        // With no active jobs, new submissions would be allowed.
        assertEquals(0, jobService.activeJobs(startJobDTO.getOrganization()));
    }

    @Test
    void testPatientExportDuplicateSubmissionWithDifferentClient() {

        List<Job> jobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        for (Job job : jobs) {
            job.setOrganization("bogus_org");
            jobRepository.saveAndFlush(job);
            dataSetup.queueForCleanup(job);
        }

        assertEquals(0, jobService.activeJobs(startJobDTO.getOrganization()));
    }
}