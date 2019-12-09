package gov.cms.ab2d.audit.cleanup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
@RequiredArgsConstructor
public class FileDeletionJob extends QuartzJobBean {

    private final FileDeletionService fileDeletionService;

    /**
     * Delete all files that are in the efs mount that are older than the TTL variable
     * @param jobExecutionContext
     */
    @Override
    public void executeInternal(JobExecutionContext jobExecutionContext) {
        fileDeletionService.deleteFiles();
    }
}
