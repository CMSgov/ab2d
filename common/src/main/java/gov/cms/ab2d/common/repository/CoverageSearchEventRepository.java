package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.CoverageSearchEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoverageSearchEventRepository extends JpaRepository<CoverageSearchEvent, Long> {
}
