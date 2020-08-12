package gov.cms.ab2d.hpms.service;

import java.util.List;

public interface AttestationService {
    List<String> retrieveAttestations(List<String> contractIds);
}
