package gov.cms.ab2d.common.util;

import gov.cms.ab2d.common.health.*;
import lombok.experimental.UtilityClass;

import javax.sql.DataSource;
import java.util.List;

@UtilityClass
public class HealthCheckUtil {

    public static boolean healthy(DataSource dataSource, String efsMount, int memory, List<String> urls) {
        return
            // Check for access to the database
            DatabaseAvailable.isDbAvailable(dataSource) &&
            // EFS Mount can be written to
            FileSystemCheck.canWriteFile(efsMount, true) &&

            // We're not out of memory
            !MemoryUtilization.outOfMemory(memory) &&
            // Internet is accessible
            UrlAvailable.isAnyAvailable(urls) &&
            // We can log
            LoggingAvailable.canLog();
    }

}
