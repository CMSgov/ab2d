package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.CoverageDelta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoverageDeltaTestRepository extends JpaRepository<CoverageDelta, Long>  {
}
