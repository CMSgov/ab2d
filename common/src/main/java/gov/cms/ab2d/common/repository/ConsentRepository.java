package gov.cms.ab2d.common.repository;


import gov.cms.ab2d.common.model.Consent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, Long> {

}
