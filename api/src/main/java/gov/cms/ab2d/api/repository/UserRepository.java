package gov.cms.ab2d.api.repository;

import gov.cms.ab2d.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Nullable
    User findByUserID(@Nullable String userID);
}
