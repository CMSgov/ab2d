package gov.cms.ab2d.coverage.repository;

import gov.cms.ab2d.coverage.model.CoverageDelta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoverageDeltaTestRepository extends JpaRepository<CoverageDelta, Long>  {
}
