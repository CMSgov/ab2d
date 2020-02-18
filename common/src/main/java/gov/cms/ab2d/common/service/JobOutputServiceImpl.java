package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class JobOutputServiceImpl implements JobOutputService {

    @Autowired
    private JobOutputRepository jobOutputRepository;

    public JobOutput updateJobOutput(JobOutput jobOutput) {
        return jobOutputRepository.save(jobOutput);
    }

    public JobOutput findByFilePathAndJob(String fileName, Job job) {
        return jobOutputRepository.findByFilePathAndJob(fileName, job).orElseThrow(() -> {
            log.error("JobOutput with fileName {} was not able to be found for job {}", fileName, job.getJobUuid());
            throw new ResourceNotFoundException("JobOutput with fileName " + fileName + " was not able to be found" +
                    " for job " + job.getJobUuid());
        });
    }
}
