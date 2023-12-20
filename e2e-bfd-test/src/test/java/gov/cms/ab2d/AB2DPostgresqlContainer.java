package gov.cms.ab2d;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import java.io.IOException;
import java.util.Collections;

public class AB2DPostgresqlContainer extends PostgreSQLContainer<AB2DPostgresqlContainer> {

    private static final ImageFromDockerfile DOCKER_IMAGE = new ImageFromDockerfile()
        .withDockerfileFromBuilder(builder ->
            builder
                .from("postgres:15.2")
                .run("apt-get update")
                .run("touch /docker-entrypoint-initdb.d/init_pg_cron.sh")
                .run("echo \"#!/bin/sh\"\n >> /docker-entrypoint-initdb.d/init_pg_cron.sh")
                .run("echo \"# Configure PG Cron\"\n >> /docker-entrypoint-initdb.d/init_pg_cron.sh")
                .run("echo \"echo shared_preload_libraries = 'pg_cron' >> /var/lib/postgresql/data/postgresql.conf\"\n >> /docker-entrypoint-initdb.d/init_pg_cron.sh")
                .run("echo \"echo cron.database_name = 'test' >> /var/lib/postgresql/data/postgresql.conf\"\n >> /docker-entrypoint-initdb.d/init_pg_cron.sh")
                .run("echo \"# Required to load pg_cron\"\n >> /docker-entrypoint-initdb.d/init_pg_cron.sh")
                .run("echo \"pg_ctl restart\"\n >> /docker-entrypoint-initdb.d/init_pg_cron.sh")
                .run("chmod +x /docker-entrypoint-initdb.d/init_pg_cron.sh")
                .run("apt-get install -y curl postgresql-15-cron")
                .build()
        );

    private static final DockerImageName IMAGE_NAME = DockerImageName.parse(DOCKER_IMAGE.get())
        .asCompatibleSubstituteFor(PostgreSQLContainer.IMAGE);

    public AB2DPostgresqlContainer() {
        super(IMAGE_NAME);
    }

    private void execOnStart() {

        try {
            String initDbPath = "/docker-entrypoint-initdb.d/init_pg_cron.sh";
            String pgcronConfigPath = "/var/lib/postgresql/data/postgresql.conf";

            ExecResult catResult = super.execInContainer("cat", initDbPath);
            System.out.println("CAT RESULT = ");
            System.out.println(catResult.getStdout());

            ExecResult pgcronCatResult = super.execInContainer("cat", pgcronConfigPath);
            System.out.println("pg_cron CAT RESULT = ");
            System.out.println(pgcronCatResult.getStdout());

        } catch (UnsupportedOperationException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        super.withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"));
        super.start();

        // For debugging purposes, uncomment
        // execOnStart();

        System.setProperty("DB_URL", this.getJdbcUrl());
        System.setProperty("DB_USERNAME", this.getUsername());
        System.setProperty("DB_PASSWORD", this.getPassword());

    }

    @Override
    public void stop() {
        // Don't call stop between shutdown for now
    }
}
