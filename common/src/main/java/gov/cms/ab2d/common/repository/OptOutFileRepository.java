package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.OptOutFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OptOutFileRepository extends JpaRepository<OptOutFile, Long> {

    Optional<OptOutFile> findByFilename(String filename);
}
