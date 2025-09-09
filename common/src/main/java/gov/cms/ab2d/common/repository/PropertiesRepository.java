package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertiesRepository extends JpaRepository<Property, Long> {
    Optional<Property> findByKey(String key);
}
