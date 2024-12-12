package gov.cms.ab2d.coverage.util;

import java.util.Collections;
import org.testcontainers.containers.PostgreSQLContainer;

public class AB2DCoveragePostgressqlContainer extends PostgreSQLContainer<AB2DCoveragePostgressqlContainer> {

    private static final String IMAGE_VERSION = "postgres:16";

    public AB2DCoveragePostgressqlContainer() {
        super(IMAGE_VERSION);
    }

    @Override
    public void start() {
        super.withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"));
        super.start();
        System.setProperty("DB_URL", this.getJdbcUrl());
        System.setProperty("DB_USERNAME", this.getUsername());
        System.setProperty("DB_PASSWORD", this.getPassword());
    }

    @Override
    public void stop() {
        // Don't call stop between shutdown for now
    }
}
