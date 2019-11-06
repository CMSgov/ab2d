package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Attestation;
import gov.cms.ab2d.common.model.Contract;

public interface AttestationService {

    Attestation saveAttestation(Attestation attestation);

    Attestation getMostRecentAttestationFromContract(Contract contract);
}
