package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.worker.processor.JobMeasure;
import gov.cms.ab2d.worker.processor.JobProgressUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Primary    // Insures selection for tests, this bean is not there for deployed code.
@Service
public class JobChannelStubServiceImpl implements JobChannelService {

    private final JobProgressUpdateService jobProgressUpdateService;

    @Override
    public void sendUpdate(String jobUuid, JobMeasure measure, long value) {
        jobProgressUpdateService.addMeasure(jobUuid, measure, value);
    }
}

