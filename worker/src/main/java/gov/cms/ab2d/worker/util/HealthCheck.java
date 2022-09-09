package gov.cms.ab2d.worker.util;

import gov.cms.ab2d.common.health.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

@Component
@Slf4j
public class HealthCheck {

    private final DataSource dataSource;
    private final String efsMount;
    private final int memory;
    private final List<String> urls;

    public HealthCheck(DataSource dataSource, @Value("${efs.mount}") String efsMount,
                       @Value("${health.requiredSpareMemoryInMB}") int memory,
                       @Value("#{'${health.urlsToCheck}'.split(',')}") List<String> urls) {
        this.dataSource = dataSource;
        this.efsMount = efsMount;
        this.memory = memory;
        this.urls = urls;
    }

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


    public boolean dbHealth() {
        return DatabaseAvailable.isDbAvailable(dataSource);
    }

    public boolean efsHealth() {
        return FileSystemCheck.canWriteFile(efsMount, true);
    }

    public boolean memoryHealth() {
        return !MemoryUtilization.outOfMemory(memory);
    }
}
