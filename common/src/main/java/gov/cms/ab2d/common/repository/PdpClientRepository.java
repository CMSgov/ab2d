package gov.cms.ab2d.common.repository;

import gov.cms.ab2d.common.model.PdpClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PdpClientRepository extends JpaRepository<PdpClient, Long> {

    @Nullable
    PdpClient findByClientId(@Nullable String clientId);

    @Query("SELECT u FROM PdpClient u WHERE u.contract.contractNumber = :contractNumber")
    List<PdpClient> findByContract(@NonNull String contractNumber);
}
