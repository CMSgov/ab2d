package gov.cms.ab2d.api.security;


import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final PdpClientRepository pdpClientRepository;

    @Override
    public UserDetails loadUserByUsername(String clientId) throws UsernameNotFoundException {
        PdpClient pdpClient = pdpClientRepository.findByClientId(clientId);
        if (pdpClient == null) {
            throw new UsernameNotFoundException("Client not found for client id");
        }
        return pdpClient;
    }
}
