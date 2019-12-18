package gov.cms.ab2d.optout.job;

import gov.cms.ab2d.optout.OptOutProcessor;
import lombok.RequiredArgsConstructor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

@RequiredArgsConstructor
@DisallowConcurrentExecution
public class OptoutJob extends QuartzJobBean {

    private final OptOutProcessor processor;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {

        processor.process();

    }
}
