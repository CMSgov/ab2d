package gov.cms.ab2d.common.util;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class AttestationStatusTest {

    // Add better tests in future, for now just use this to meet coverage checks, it's being covered by HPMS module
    @Test
    public void testAttestationStatus() {
        AttestationStatus attestationStatus = AttestationStatus.WITHOUT_ATTESTATION;
        Assert.assertEquals("Without Attestation", attestationStatus.getValue());

        AttestationStatus attestationStatusAttested = AttestationStatus.ATTESTED;
        Assert.assertEquals("Attested", attestationStatusAttested.getValue());
    }
}
