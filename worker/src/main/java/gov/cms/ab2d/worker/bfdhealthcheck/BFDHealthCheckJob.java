package gov.cms.ab2d.worker.bfdhealthcheck;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import static gov.cms.ab2d.fhir.FhirVersion.STU3;

@Slf4j
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class BFDHealthCheckJob extends QuartzJobBean {

    private BFDHealthCheck bfdHealthCheck;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        bfdHealthCheck.checkBFDHealth();
    }
}
