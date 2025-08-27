package gov.cms.ab2d.contracts.hmsapi;

import lombok.Data;

@Data
public class HPMSAuthResponse {

    private String accessToken;
    private int expires;
}
