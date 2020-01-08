package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Sponsor;

import java.util.Optional;

public interface SponsorService {

    Optional<Sponsor> findByHpmsIdAndParent(Integer hpmsId, Sponsor parentId);

    Sponsor saveSponsor(Sponsor sponsor);

    Sponsor findSponsorById(Long id);

    Sponsor findByHpmsIdAndOrgName(Integer hpmsId, String orgName);
}
