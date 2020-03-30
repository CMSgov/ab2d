package gov.cms.ab2d.api.security;

import org.json.JSONException;

import java.io.IOException;
import java.net.URISyntaxException;

public interface OktaTokenGenerator {

    String generateToken(String clientID, String clientSecret) throws URISyntaxException, IOException, InterruptedException, JSONException;
}
