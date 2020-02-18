package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.Constants.ADMIN_PREFIX;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
public class AdminAPIPropertiesTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUtil testUtil;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private static final String PROPERTIES_URL = "/properties";

    private String token;

    @BeforeEach
    public void setup() throws JwtVerificationException {
        token = testUtil.setupToken(List.of(ADMIN_ROLE, SPONSOR_ROLE));
    }

    @Test
    public void testRetrieveProperties() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX + ADMIN_PREFIX + PROPERTIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(200, mvcResult.getResponse().getStatus());

        String result = mvcResult.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        List<PropertiesDTO> propertiesDTOs = mapper.readValue(result, new TypeReference<List<PropertiesDTO>>(){});

        Assert.assertEquals(3, propertiesDTOs.size());
        for(PropertiesDTO propertiesDTO : propertiesDTOs) {
            Assert.assertEquals("", propertiesDTO.getKey());
        }
    }
}
