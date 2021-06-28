package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.dto.PdpClientDTO;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.PdpClient;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface PdpClientService {

    /**
     * Find all {@link Contract}s that have credentials which are enabled. This method does not
     * filter fake contracts Z* from real contracts S*
     */
    List<Contract> getAllEnabledContracts();

    PdpClient getCurrentClient();

    PdpClient getClientById(String clientId);

    PdpClientDTO createClient(PdpClientDTO pdpClientDTO);

    PdpClientDTO updateClient(PdpClientDTO pdpClientDTO);

    void setupClientImpersonation(String contractNumber, HttpServletRequest request);

    void setupClientAndRolesInSecurityContext(PdpClient pdpClient, HttpServletRequest request);

    List<GrantedAuthority> getGrantedAuth(PdpClient pdpClient);

    PdpClientDTO enableClient(String contractNumber);

    PdpClientDTO disableClient(String contractNumber);

    PdpClientDTO getClient(String contractNumber);
}
