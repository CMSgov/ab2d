package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;

import static gov.cms.ab2d.common.util.Constants.OKTA_PROXY_ENDPOINT;
import static gov.cms.ab2d.common.util.Constants.SPONSOR_ROLE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"api.okta-jwt-issuer=https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297"})
@AutoConfigureMockMvc
@Testcontainers
public class OktaProxyAPITests {

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testOktaProxyCall() throws Exception {
        this.mockMvc.perform(post(OKTA_PROXY_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(EntityUtils.toString(new UrlEncodedFormEntity(Arrays.asList(
                        new BasicNameValuePair("clientID", "0oa2t0lsrdZw5uWRx297"),
                        new BasicNameValuePair("clientSecret", "HHduWG6LogIvDIQuWgp3Zlo9OYMValTtH5OBcuHw")
                )))))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.accessToken", Is.is(Matchers.notNullValue())));
    }

    @Test
    public void testOktaProxyCallBadParams() throws Exception {
        this.mockMvc.perform(post(OKTA_PROXY_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(EntityUtils.toString(new UrlEncodedFormEntity(Arrays.asList(
                        new BasicNameValuePair("clientID", "BadParam"),
                        new BasicNameValuePair("clientSecret", "BadParam")
                )))))
                .andExpect(status().is(400));
    }
}
