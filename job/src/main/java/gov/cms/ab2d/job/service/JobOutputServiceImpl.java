package gov.cms.ab2d.job.service;

import gov.cms.ab2d.job.dto.StaleJob;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobOutput;
import gov.cms.ab2d.job.repository.JobOutputRepository;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
@Service
@Transactional
public class JobOutputServiceImpl implements JobOutputService {

    private final JobOutputRepository jobOutputRepository;

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
