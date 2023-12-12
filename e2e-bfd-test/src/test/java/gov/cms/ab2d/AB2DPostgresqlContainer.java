package gov.cms.ab2d;

import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Collections;

public class AB2DPostgresqlContainer extends PostgreSQLContainer<AB2DPostgresqlContainer> {

    // We should really move this to some sort of config right?
    // Right now this is separate from docker-compose image version and that feels dirty to me
    // This is also duplicated across other files...which seems like a potential maintainence concern.
    private static final String IMAGE_VERSION = "postgres:15";

    public AB2DPostgresqlContainer() {
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
