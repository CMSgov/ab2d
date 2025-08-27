package gov.cms.ab2d.contracts.repository;

import gov.cms.ab2d.contracts.model.Property;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

//TODO Delete once property service is released
public interface PropertiesRepository extends JpaRepository<Property, Long> {
    Optional<Property> findByKey(String key);
}
