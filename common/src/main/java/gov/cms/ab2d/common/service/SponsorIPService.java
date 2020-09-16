package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.dto.SponsorDTO;
import gov.cms.ab2d.common.dto.SponsorIPDTO;

public interface SponsorIPService {

    SponsorIPDTO addIPAddress(SponsorIPDTO sponsorIPDTO);

    SponsorIPDTO removeIPAddresses(SponsorIPDTO sponsorIPDTO);

    SponsorIPDTO getIPs(SponsorDTO sponsorDTO);
}
