package gov.cms.ab2d.coverage.util;

import java.util.Collections;
import org.testcontainers.containers.PostgreSQLContainer;

public class AB2DPostgresqlContainer extends PostgreSQLContainer<AB2DPostgresqlContainer> {

    private static final String IMAGE_VERSION = "postgres:11";

    public AB2DPostgresqlContainer() {
        super(IMAGE_VERSION);
    }

    @Override
    public void start() {
        super.withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"));
        super.start();
        System.out.println("LOL: " + this.getJdbcUrl() + "   :   " + this.getPassword());

        System.setProperty("DB_URL", this.getJdbcUrl());
        System.setProperty("DB_USERNAME","ab2d");
        System.setProperty("DB_PASSWORD", "ab2d");
    }

    @Override
    public void stop() {
        // Don't call stop between shutdown for now
    }
}
