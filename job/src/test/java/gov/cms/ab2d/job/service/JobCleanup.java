package gov.cms.ab2d.job.service;

import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public abstract class JobCleanup {

    @Autowired
    JobRepository jobRepository;

    private final List<Job> jobsToCleanup = new ArrayList<>();

    protected void addJobForCleanup(Job job) {
        jobsToCleanup.add(job);
    }

    protected void jobCleanup() {
        jobsToCleanup.forEach(job -> {
            Job foundJob = jobRepository.findByJobUuid(job.getJobUuid());
            jobRepository.delete(foundJob);
        });
        jobRepository.flush();
        jobsToCleanup.clear();
    }
}
