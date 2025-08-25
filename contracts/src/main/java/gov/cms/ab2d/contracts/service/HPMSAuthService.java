package gov.cms.ab2d.contracts.service;

import org.springframework.http.HttpHeaders;

public interface HPMSAuthService {

    void buildAuthHeaders(HttpHeaders headers);
}
