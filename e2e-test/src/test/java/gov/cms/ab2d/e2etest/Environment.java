package gov.cms.ab2d.e2etest;

import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Possible environments which the e2e-test might be run in
 */
public enum Environment {

    LOCAL(Ab2dEnvironment.LOCAL, "local-config.yml", List.of("docker-compose.yml")),
    CI(Ab2dEnvironment.LOCAL, "local-config.yml", List.of("docker-compose.yml", "docker-compose.jenkins.yml")),
    DEV(Ab2dEnvironment.DEV, "dev-config.yml", Collections.emptyList()),
    SBX(Ab2dEnvironment.SANDBOX, "sbx-config.yml", Collections.emptyList()),
    IMPL(Ab2dEnvironment.IMPL, "impl-config.yml", Collections.emptyList()),
    PROD(Ab2dEnvironment.PRODUCTION, "prod-config.yml", Collections.emptyList());

    private final Ab2dEnvironment ab2dEnvironment;
    private final String configName;

    // Docker compose can actually accept a list of docker-compose yaml files
    // and overrides variables provided by those files in the order they are listed in
    // For CI docker-compose.yml is loaded first then the overrides in docker-compose.jenkins.yml are applied
    // For more information see the documentation here: https://docs.docker.com/compose/extends/
    private final List<String> dockerComposeFiles;

    Environment(Ab2dEnvironment ab2dEnvironment, String configName, List<String> dockerComposeFiles) {
        this.ab2dEnvironment = ab2dEnvironment;
        this.configName = configName;
        this.dockerComposeFiles = dockerComposeFiles;
    }

    public String getConfigName() {
        return configName;
    }

    /**
     * Get an array of compose files in the order those files are to be loaded.
     *
     * The {@link org.testcontainers.containers.DockerComposeContainer} accepts an array of compose
     * files so instead of returning a list convert to an array.
     */
    public File[] getComposeFiles() {
        List<File> composeFiles = dockerComposeFiles.stream()
                .map(compose -> ".." + File.separator + compose).map(File::new).collect(Collectors.toList());

        File[] composeFilesArray = new File[composeFiles.size()];
        composeFiles.toArray(composeFilesArray);

        return composeFilesArray;
    }

    public Ab2dEnvironment getAb2dEnvironment() {
        return ab2dEnvironment;
    }

    /**
     * Check whether environment expects to run a docker-compose
     */
    public boolean hasComposeFiles() {
        return !dockerComposeFiles.isEmpty();
    }
}
