package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.hpms.hmsapi.HPMSAttestationsHolder;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizations;

import java.util.List;
import java.util.function.Consumer;

public interface HPMSFetcher {

    void retrieveSponsorInfo(Consumer<HPMSOrganizations> hpmsOrgCallback);

    void retrieveAttestationInfo(Consumer<HPMSAttestationsHolder> hpmsAttestationCallback, List<String> contractIds);
}
