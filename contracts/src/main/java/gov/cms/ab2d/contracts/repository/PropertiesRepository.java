package gov.cms.ab2d.contracts.repository;

import gov.cms.ab2d.contracts.model.Property;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertiesRepository extends JpaRepository<Property, Long> {
    Optional<Property> findByKey(String key);
}
