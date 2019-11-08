package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Attestation;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.AttestationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;

@Service
@Transactional
public class AttestationServiceImpl implements AttestationService {

    @Autowired
    private AttestationRepository attestationRepository;

    @Override
    public Attestation saveAttestation(Attestation attestation, @Nullable OffsetDateTime attestedOn) {
        attestation.setAttestedOn(attestedOn);

        return attestationRepository.saveAndFlush(attestation);
    }

    @Override
    public Attestation getAttestationFromContract(Contract contract) {
        return attestationRepository.findOneByContract(contract);
    }
}
