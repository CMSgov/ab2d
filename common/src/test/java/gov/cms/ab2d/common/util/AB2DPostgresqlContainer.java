package gov.cms.ab2d.common.util;

import org.testcontainers.containers.PostgreSQLContainer;

public class AB2DPostgresqlContainer extends PostgreSQLContainer<AB2DPostgresqlContainer> {

    private static final String IMAGE_VERSION = "postgres:11";

    public AB2DPostgresqlContainer() {
        super(IMAGE_VERSION);
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("DB_URL", this.getJdbcUrl());
        System.setProperty("DB_USERNAME", this.getUsername());
        System.setProperty("DB_PASSWORD", this.getPassword());
    }

    @Override
    public void stop() {
        //super.stop();
    }
}
