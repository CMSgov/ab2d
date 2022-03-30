package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.dto.StaleJob;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Override
    public Map<StaleJob, List<String>> expiredDownloadableFiles(int minutesInterval) {
        return jobOutputRepository.findByDownloadExpiredAndJobExpired(minutesInterval).orElse(new ArrayList<>())
                .stream()
                .collect(Collectors.groupingBy(output ->
                                new StaleJob(output.getJob().getJobUuid(), output.getJob().getOrganization()),
                        Collectors.mapping(JobOutput::getFilePath, Collectors.toList())));
    }

}
