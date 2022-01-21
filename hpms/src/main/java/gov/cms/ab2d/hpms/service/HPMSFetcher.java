package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.hpms.hmsapi.HPMSAttestation;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizationInfo;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public interface HPMSFetcher {

    void retrieveSponsorInfo(Consumer<List<HPMSOrganizationInfo>> hpmsOrgCallback);

    void retrieveAttestationInfo(Consumer<Set<HPMSAttestation>> hpmsAttestationCallback, List<String> contractIds);
}
