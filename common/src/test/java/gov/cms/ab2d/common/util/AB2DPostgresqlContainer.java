package gov.cms.ab2d.common.util;

import org.testcontainers.containers.PostgreSQLContainer;

public class AB2DPostgresqlContainer extends PostgreSQLContainer<AB2DPostgresqlContainer> {
    private static final String IMAGE_VERSION = "postgres:11";
    private static AB2DPostgresqlContainer container;

    private AB2DPostgresqlContainer() {
        super(IMAGE_VERSION);
    }

    public static AB2DPostgresqlContainer getInstance() {
        if (container == null) {
            container = new AB2DPostgresqlContainer();
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("DB_URL", container.getJdbcUrl());
        System.setProperty("DB_USERNAME", container.getUsername());
        System.setProperty("DB_PASSWORD", container.getPassword());
    }

    @Override
    public void stop() {
        //do nothing, JVM handles shut down
    }
}
