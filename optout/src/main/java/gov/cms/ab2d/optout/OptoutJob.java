package gov.cms.ab2d.optout;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
@RequiredArgsConstructor
public class OptoutJob extends QuartzJobBean {

    private static int runCount = 0;

    private final OptOutProcessor processor;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {

        processor.process();

    }
}
