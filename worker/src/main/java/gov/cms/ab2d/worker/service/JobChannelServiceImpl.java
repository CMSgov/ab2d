package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.worker.processor.JobMeasure;
import gov.cms.ab2d.worker.processor.JobProgressUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobChannelServiceImpl implements JobChannelService {

    private final JobProgressUpdateService jobProgressUpdateService;

    @Override
    public void sendUpdate(String jobUuid, JobMeasure measure, long value) {
        jobProgressUpdateService.addMeasure(jobUuid, measure, value);
    }
}
