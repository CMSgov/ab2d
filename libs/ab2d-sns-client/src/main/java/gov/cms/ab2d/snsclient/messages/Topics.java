package gov.cms.ab2d.snsclient.messages;

import lombok.Getter;

@Getter
public enum Topics {
    COVERAGE_COUNTS("coverage-count");

    private final String value;

    Topics(String value) {
        this.value = value;
    }
}
