package gov.cms.ab2d.worker.util;

import gov.cms.ab2d.common.health.*;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

import static gov.cms.ab2d.eventclient.config.Ab2dEnvironment.PUBLIC_LIST;

@Component
@Slf4j
public class HealthCheck {

    private final DataSource dataSource;
    private final String efsMount;
    private final int memory;
    private final List<String> urls;
    private final PropertiesServiceAvailable propertiesServiceAvailable;
    private final Ab2dEnvironment ab2dEnvironment;

    public HealthCheck(DataSource dataSource, @Value("${efs.mount}") String efsMount,
                       @Value("${health.requiredSpareMemoryInMB}") int memory,
                       @Value("#{'${health.urlsToCheck}'.split(',')}") List<String> urls,
                       @Value("${execution.env}") String ab2dEnv,
                       PropertiesServiceAvailable propertiesServiceAvailable) {
        this.dataSource = dataSource;
        this.efsMount = efsMount;
        this.memory = memory;
        this.urls = urls;
        this.propertiesServiceAvailable = propertiesServiceAvailable;
        this.ab2dEnvironment = Ab2dEnvironment.fromName(ab2dEnv);
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
                LoggingAvailable.canLog() &&
                propertiesServiceAvailable.isAvailable(PUBLIC_LIST.contains(this.ab2dEnvironment));
    }
}
