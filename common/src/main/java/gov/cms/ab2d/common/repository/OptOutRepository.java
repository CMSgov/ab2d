package gov.cms.ab2d.common.repository;


import gov.cms.ab2d.common.model.OptOut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OptOutRepository extends JpaRepository<OptOut, Long> {

    List<OptOut> findByCcwId(String ccwId);

    Optional<OptOut> findByCcwIdAndHicn(String ccwId, String hicn);
}
