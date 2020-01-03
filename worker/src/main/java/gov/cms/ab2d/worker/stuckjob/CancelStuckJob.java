package gov.cms.ab2d.worker.stuckjob;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class CancelStuckJob extends QuartzJobBean {

    private final CancelStuckJobsProcessor processor;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {

        processor.process();

    }
}
