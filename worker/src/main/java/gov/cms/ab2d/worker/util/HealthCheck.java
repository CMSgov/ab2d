package gov.cms.ab2d.worker.util;

import gov.cms.ab2d.common.health.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

@Component
@Slf4j
public class HealthCheck {
    @Autowired
    private DataSource dataSource;

    @Value("${efs.mount}")
    private String efsMount;

    @Value("${health.requiredSpareMemoryInMB}")
    private int memory;

    @Value("#{'${health.urlsToCheck}'.split(',')}")
    private List<String> urls;

    public boolean healthy() {
        return
                // Check for access to the database
                DatabaseAvailable.isDbAvailable(dataSource) &&
                // EFS Mount can be written to
                FileSystemCheck.canWriteFile(efsMount, true) &&
                // Can write to home directory
                FileSystemCheck.canWriteFile(".", false) &&
                // We're not out of memory
                !MemoryUtilization.outOfMemory(memory) &&
                // Internet is accessible
                UrlAvailable.isAnyAvailable(urls) &&
                // We can log
                LoggingAvailable.canLog();
    }
}
