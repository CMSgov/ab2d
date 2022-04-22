package gov.cms.ab2d.eventlogger;

import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Collections;

public class AB2DPostgresqlContainer extends PostgreSQLContainer<AB2DPostgresqlContainer> {

    private static final String IMAGE_VERSION = "postgres:11";

    public AB2DPostgresqlContainer() {
        super(IMAGE_VERSION);
        withDatabaseName("event");

    }

    @Override
    public void start() {
        super.withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"));
        super.start();
        System.out.println("lol: " +this.getJdbcUrl() + this.getUsername() + this.getPassword());
        System.setProperty("DB_URL", this.getJdbcUrl());
        System.setProperty("DB_USERNAME", this.getUsername());
        System.setProperty("DB_PASSWORD", this.getPassword());
    }

    @Override
    public void stop() {
        // Don't call stop between shutdown for now
    }
}
