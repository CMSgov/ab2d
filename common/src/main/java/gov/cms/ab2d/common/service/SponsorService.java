package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Sponsor;

import java.util.Optional;

public interface SponsorService {

    Optional<Sponsor> getSponsorByHpmsId(Integer hpmsId);

    Sponsor saveSponsor(Sponsor sponsor);
}
