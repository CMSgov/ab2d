package gov.cms.ab2d.api.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OktaProxyResponse {

    private final String accessToken;
}
