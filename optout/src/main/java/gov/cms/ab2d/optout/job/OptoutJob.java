package gov.cms.ab2d.optout.job;

import gov.cms.ab2d.optout.OptOutProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class OptoutJob extends QuartzJobBean {

    private final OptOutProcessor processor;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("import opt-out data - start ...");

        processor.process();

        log.info("import opt-out data - DONE.");
    }
}
