package gov.cms.ab2d.common.repository;


import gov.cms.ab2d.common.model.OptOut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptOutRepository extends JpaRepository<OptOut, Long> {

    List<OptOut> findByHicn(String hicn);
}
