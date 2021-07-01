package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.config.Mapping;
import gov.cms.ab2d.common.dto.PdpClientDTO;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static gov.cms.ab2d.common.util.Constants.SPONSOR_ROLE;
import static java.util.stream.Collectors.toList;

/**
 * Just gets the current client from the authentication context.
 */
@AllArgsConstructor
@Transactional
@Service
@Slf4j
public class PdpClientServiceImpl implements PdpClientService {

    private final PdpClientRepository pdpClientRepository;

    private final Mapping mapping;

    @Override
    public List<Contract> getAllEnabledContracts() {
        return pdpClientRepository.findAllByEnabledTrue().stream()
                .filter(client -> client.getContract().getAttestedOn() != null)
                .filter(this::hasSponsorRole)
                .map(PdpClient::getContract).collect(toList());
    }

    private boolean hasSponsorRole(PdpClient client) {
        for (Role role : client.getRoles()) {
            if (SPONSOR_ROLE.equals(role.getName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public PdpClient getCurrentClient() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? pdpClientRepository
                .findByClientId(
                        auth.getPrincipal() instanceof String ? (String) auth.getPrincipal() :
                                ((org.springframework.security.core.userdetails.User) auth
                                        .getPrincipal())
                                        .getUsername()) : null;
    }

    /**
     * The username used for Spring's authentication is actually {@link PdpClient#getClientId()}
     * @param clientId client id authorized for the system
     * @return {@link PdpClient}
     */
    @Override
    public PdpClient getClientById(String clientId) {
        PdpClient pdpClient = pdpClientRepository.findByClientId(clientId);
        if (pdpClient == null) {
            String clientNotPresentMsg = "Client is not present in our database";
            log.error(clientNotPresentMsg);
            throw new ResourceNotFoundException(clientNotPresentMsg);
        } else {
            return pdpClient;
        }
    }

    @Override
    public PdpClientDTO createClient(PdpClientDTO pdpClientDTO) {
        PdpClient pdpClient = mapping.getModelMapper().map(pdpClientDTO, PdpClient.class);
        PdpClient createdPdpClient = pdpClientRepository.saveAndFlush(pdpClient);
        return mapping.getModelMapper().map(createdPdpClient, PdpClientDTO.class);
    }

    @Override
    public PdpClientDTO updateClient(PdpClientDTO pdpClientDTO) {
        // When updating there needs to be verification that the client exists
        PdpClient pdpClient = getClientById(pdpClientDTO.getClientId());
        pdpClientDTO.setId(pdpClient.getId());
        PdpClient mappedPdpClient = mapping.getModelMapper().map(pdpClientDTO, PdpClient.class);
        PdpClient updatedPdpClient = pdpClientRepository.saveAndFlush(mappedPdpClient);
        return mapping.getModelMapper().map(updatedPdpClient, PdpClientDTO.class);
    }

    @Override
    public void setupClientImpersonation(String contractNumber, HttpServletRequest request) {
        PdpClient pdpClient = getClientByContract(contractNumber);
        log.info("Admin client is impersonating client {}", pdpClient.getId());
        setupClientAndRolesInSecurityContext(pdpClient, request);
    }

    private PdpClient getClientByContract(String contractNumber) {
        // May be more than one client mapping to a contract
        // since we removed the constraint
        List<PdpClient> pdpClients = pdpClientRepository.findByContract(contractNumber);

        if (pdpClients.isEmpty()) {
            String clientNotPresentMsg = "Client is not present in our database";
            log.error(clientNotPresentMsg);
            throw new ResourceNotFoundException(clientNotPresentMsg);
        }

        if (pdpClients.size() > 1) {
            log.warn("{} clients map to {} contract. This should not be possible.", pdpClients.size(), contractNumber);
        }

        return pdpClients.get(0);
    }

    @Override
    public void setupClientAndRolesInSecurityContext(PdpClient pdpClient, HttpServletRequest request) {
        List<GrantedAuthority> authorities = getGrantedAuth(pdpClient);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                pdpClient.getClientId(), null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        log.info("Successfully logged in");
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Retrieve the list of granted authorities from the client's roles
     *
     * @param pdpClient - the client
     * @return - the granted authorities
     */
    @Override
    public List<GrantedAuthority> getGrantedAuth(PdpClient pdpClient) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Role role : pdpClient.getRoles()) {
            log.info("Adding role {}", role.getName());
            authorities.add(new SimpleGrantedAuthority(role.getName()));
        }
        return authorities;
    }

    @Override
    public PdpClientDTO enableClient(String contractNumber) {
        return updateClientStatus(contractNumber, true);
    }

    @Override
    public PdpClientDTO disableClient(String contractNumber) {
        return updateClientStatus(contractNumber, false);
    }

    @Override
    public PdpClientDTO getClient(String contractNumber) {
        PdpClient pdpClient = getClientByContract(contractNumber);
        return mapping.getModelMapper().map(pdpClient, PdpClientDTO.class);
    }

    private PdpClientDTO updateClientStatus(String contractNumber, boolean enabled) {
        PdpClient pdpClient = getClientByContract(contractNumber);
        pdpClient.setEnabled(enabled);
        PdpClient updatedPdpClient = pdpClientRepository.saveAndFlush(pdpClient);
        return mapping.getModelMapper().map(updatedPdpClient, PdpClientDTO.class);
    }
}
