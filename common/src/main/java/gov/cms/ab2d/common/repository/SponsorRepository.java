package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.Sponsor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SponsorRepository extends JpaRepository<Sponsor, Long> {

    Optional<Sponsor> findByHpmsIdAndParent(Integer hpmsId, Sponsor parentId);

    List<Sponsor> findByParent(Sponsor parentId);
}
