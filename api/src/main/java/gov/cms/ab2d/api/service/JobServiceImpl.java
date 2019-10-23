package gov.cms.ab2d.api.service;


import gov.cms.ab2d.api.repository.JobRepository;
import gov.cms.ab2d.domain.Job;
import gov.cms.ab2d.domain.JobStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class JobServiceImpl implements JobService {

    @Autowired
    private UserService userService;

    @Autowired
    private JobRepository jobRepository;

    public static final String INITIAL_JOB_STATUS_MESSAGE = "0%";

    public Job createJob(String resourceTypes, String url) {
        Job job = new Job();
        job.setResourceTypes(resourceTypes);
        job.setJobID(UUID.randomUUID().toString());
        job.setRequestURL(url);
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage(INITIAL_JOB_STATUS_MESSAGE);
        job.setCreatedAt(LocalDateTime.now());
        job.setProgress(0);
        job.setUser(userService.getCurrentUser());

        return jobRepository.save(job);
    }

    public Job getJobByJobID(String jobID) {
        Job job = jobRepository.findByJobID(jobID);
        if (job == null) {
            throw new ResourceNotFoundException("No job with jobID " +  jobID + " was found");
        }

        return job;
    }

    public Job updateJob(Job job) {
        return jobRepository.save(job);
    }
}
