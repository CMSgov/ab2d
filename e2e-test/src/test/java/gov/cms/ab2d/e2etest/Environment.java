package gov.cms.ab2d.e2etest;

import lombok.Getter;

@Getter
public enum Environment {

    LOCAL("local-config.yml", true),
    DEV("dev-config.yml", false),
    SBX("sbx-config.yml", false),
    IMP("imp-config.yml", false),
    PROD("prod-config.yml", false);

    private String configName;

    private boolean usesDockerCompose;

    Environment(String configName, boolean usesDockerCompose) {
        this.configName = configName;
        this.usesDockerCompose = usesDockerCompose;
    }
}
