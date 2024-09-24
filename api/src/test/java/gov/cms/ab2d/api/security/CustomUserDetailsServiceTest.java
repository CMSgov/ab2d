package gov.cms.ab2d.api.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.repository.PdpClientRepository;

class CustomUserDetailsServiceTest {

  @Test
  void testLoadUserByUsername1() {
    PdpClientRepository pdpClientRepository = mock(PdpClientRepository.class);
    CustomUserDetailsService customUserDetailsService = new CustomUserDetailsService(pdpClientRepository);
    PdpClient pdpClient = new PdpClient();
    when(pdpClientRepository.findByClientId("clientId")).thenReturn(pdpClient);
    assertEquals(
      customUserDetailsService.loadUserByUsername("clientId"),
      pdpClient
    );
  }

  @Test
  void testLoadUserByUsername2() {
    PdpClientRepository pdpClientRepository = mock(PdpClientRepository.class);
    CustomUserDetailsService customUserDetailsService = new CustomUserDetailsService(pdpClientRepository);
    when(pdpClientRepository.findByClientId(null)).thenReturn(null);
    assertThrows(
      UsernameNotFoundException.class,
      () -> {customUserDetailsService.loadUserByUsername(null);}
    );
  }

}
