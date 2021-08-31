package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.worker.processor.JobMeasure;
import gov.cms.ab2d.worker.processor.JobProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobChannelServiceImpl implements JobChannelService {

    private final JobProgressService jobProgressService;

    @Override
    public void sendUpdate(String jobId, JobMeasure measure, long value) {
        jobProgressService.addMeasure(jobId, measure, value);
    }
}
