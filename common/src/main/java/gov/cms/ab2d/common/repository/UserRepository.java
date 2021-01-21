package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Nullable
    User findByUsername(@Nullable String username);

    @Query("SELECT u FROM User u WHERE u.contract.contractNumber = :contractNumber")
    Optional<User> findByContract(@NonNull String contractNumber);
}
