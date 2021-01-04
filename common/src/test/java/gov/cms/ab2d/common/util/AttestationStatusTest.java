package gov.cms.ab2d.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttestationStatusTest {

    // Add better tests in future, for now just use this to meet coverage checks, it's being covered by HPMS module
    @Test
    void testAttestationStatus() {
        AttestationStatus attestationStatus = AttestationStatus.WITHOUT_ATTESTATION;
        assertEquals("Without Attestation", attestationStatus.getValue());

        AttestationStatus attestationStatusAttested = AttestationStatus.ATTESTED;
        assertEquals("Attested", attestationStatusAttested.getValue());
    }
}
