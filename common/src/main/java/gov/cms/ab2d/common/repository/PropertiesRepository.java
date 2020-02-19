package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.Properties;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertiesRepository extends JpaRepository<Properties, Long> {

    Optional<Properties> findByKey(String key);
}
