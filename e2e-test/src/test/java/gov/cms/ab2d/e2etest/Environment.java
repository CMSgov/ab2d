package gov.cms.ab2d.e2etest;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public enum Environment {


    LOCAL("local-config.yml", List.of("docker-compose.yml")),
    CI("local-config.yml", List.of("docker-compose.yml", "docker-compose.jenkins.yml")),
    DEV("dev-config.yml", Collections.emptyList()),
    SBX("sbx-config.yml", Collections.emptyList()),
    IMP("imp-config.yml", Collections.emptyList()),
    PROD("prod-config.yml", Collections.emptyList());

    private final String configName;

    private final List<String> dockerComposeFiles;

    Environment(String configName, List<String> dockerComposeFiles) {
        this.configName = configName;
        this.dockerComposeFiles = dockerComposeFiles;
    }

    public String getConfigName() {
        return configName;
    }

    public File[] getComposeFiles() {
        List<File> composeFiles = dockerComposeFiles.stream()
                .map(compose -> ".." + File.separator + compose).map(File::new).collect(Collectors.toList());

        File[] composeFilesArray = new File[composeFiles.size()];
        composeFiles.toArray(composeFilesArray);

        return composeFilesArray;
    }

    public boolean hasComposeFiles() {
        return !dockerComposeFiles.isEmpty();
    }
}
