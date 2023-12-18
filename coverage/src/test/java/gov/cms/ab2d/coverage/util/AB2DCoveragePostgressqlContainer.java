package gov.cms.ab2d.coverage.util;

import java.util.Collections;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

public class AB2DCoveragePostgressqlContainer extends PostgreSQLContainer<AB2DCoveragePostgressqlContainer> {

    private static final ImageFromDockerfile DOCKER_IMAGE = new ImageFromDockerfile()
        .withDockerfileFromBuilder(builder ->
            builder
                .from("postgres:15-bullseye")
                .run("apt-get update")
                .run("apt-get install -y curl postgresql-15-cron")
                .run("echo \"shared_preload_libraries = 'pg_cron'\" >> /var/lib/postgresql/data/postgresql.conf")
                .build()
        );

    private static final DockerImageName IMAGE_NAME = DockerImageName.parse(DOCKER_IMAGE.get())
        .asCompatibleSubstituteFor(PostgreSQLContainer.IMAGE);
    
    public AB2DCoveragePostgressqlContainer() {
        super(IMAGE_NAME);
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
