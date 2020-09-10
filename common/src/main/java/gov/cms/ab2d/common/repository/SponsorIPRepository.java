package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.SponsorIP;
import gov.cms.ab2d.common.model.SponsorIPID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SponsorIPRepository extends JpaRepository<SponsorIP, SponsorIPID> {

}
