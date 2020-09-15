package gov.cms.ab2d.hpms.hmsapi;

import lombok.Data;

@Data
public class HPMSAuthResponse {

    private String accessToken;
    private int expires;
}
