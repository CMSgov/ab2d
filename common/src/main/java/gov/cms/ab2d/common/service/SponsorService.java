package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Sponsor;

import javax.annotation.Nullable;
import java.util.Optional;

public interface SponsorService {

    Optional<Sponsor> getSponsorByHpmsIdAndParent(Integer hpmsId, @Nullable Sponsor parent);

    Sponsor saveSponsor(Sponsor sponsor);
}
