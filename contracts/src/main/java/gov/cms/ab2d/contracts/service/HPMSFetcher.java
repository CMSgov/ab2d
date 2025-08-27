package gov.cms.ab2d.contracts.service;

import gov.cms.ab2d.contracts.hmsapi.HPMSAttestation;
import gov.cms.ab2d.contracts.hmsapi.HPMSEnrollment;
import gov.cms.ab2d.contracts.hmsapi.HPMSOrganizationInfo;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public interface HPMSFetcher {

    List<HPMSOrganizationInfo> retrieveSponsorInfo();

    Set<HPMSAttestation> retrieveAttestationInfo(List<String> contractIds);

    Set<HPMSEnrollment> retrieveEnrollmentInfo(List<String> contractIds);
}
