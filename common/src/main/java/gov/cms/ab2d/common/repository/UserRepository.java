package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Nullable
    User findByUsername(@Nullable String username);
}
