package gov.cms.ab2d.common.util;

import lombok.Getter;

@Getter
public enum AttestationStatus {

    WITHOUT_ATTESTATION("Without Attestation"),
    ATTESTED("Attested");

    private String value;

    AttestationStatus(String value) {
        this.value = value;
    }
}
