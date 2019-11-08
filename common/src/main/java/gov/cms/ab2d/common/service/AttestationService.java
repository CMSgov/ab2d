package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Attestation;
import gov.cms.ab2d.common.model.Contract;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;

public interface AttestationService {

    Attestation saveAttestation(Attestation attestation, @Nullable OffsetDateTime attestedOn);

    Attestation getAttestationFromContract(Contract contract);
}
