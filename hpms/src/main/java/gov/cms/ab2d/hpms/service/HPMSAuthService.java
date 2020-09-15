package gov.cms.ab2d.hpms.service;

import org.springframework.http.HttpHeaders;

public interface HPMSAuthService {

    void buildAuthHeaders(HttpHeaders headers);
}
