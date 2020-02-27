package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.Assert;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.Constants.ADMIN_PREFIX;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminAPIPropertiesTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private static final String PROPERTIES_URL = "/properties";

    private String token;

    @BeforeEach
    public void setup() throws JwtVerificationException {
        contractRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        sponsorRepository.deleteAll();

        token = testUtil.setupToken(List.of(ADMIN_ROLE));
    }

    @Test
    @Order(1)
    public void testRetrieveProperties() throws Exception {
        Map<String, Object> propertyMap = new HashMap<>(){{
            put(PCP_CORE_POOL_SIZE, 10);
            put(PCP_MAX_POOL_SIZE, 150);
            put(PCP_SCALE_TO_MAX_TIME, 900);
        }};

        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX + ADMIN_PREFIX + PROPERTIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(200, mvcResult.getResponse().getStatus());

        String result = mvcResult.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        List<PropertiesDTO> propertiesDTOs = mapper.readValue(result, new TypeReference<List<PropertiesDTO>>() { } );

        Assert.assertEquals(3, propertiesDTOs.size());
        for(PropertiesDTO propertiesDTO : propertiesDTOs) {
            Object value = propertyMap.get(propertiesDTO.getKey());

            Assert.assertNotNull(value);
            Assert.assertEquals(value.toString(), propertiesDTO.getValue());
        }
    }

    @Test
    @Order(2)
    public void testUpdateProperties() throws Exception {
        List<PropertiesDTO> propertiesDTOs = new ArrayList<>();
        PropertiesDTO pcpCorePoolSizeDTO = new PropertiesDTO();
        pcpCorePoolSizeDTO.setKey(PCP_CORE_POOL_SIZE);
        pcpCorePoolSizeDTO.setValue("15");
        propertiesDTOs.add(pcpCorePoolSizeDTO);

        PropertiesDTO pcpMaxPoolSizeDTO = new PropertiesDTO();
        pcpMaxPoolSizeDTO.setKey(PCP_MAX_POOL_SIZE);
        pcpMaxPoolSizeDTO.setValue("25");
        propertiesDTOs.add(pcpMaxPoolSizeDTO);

        PropertiesDTO pcpScaleToMaxTimeDTO = new PropertiesDTO();
        pcpScaleToMaxTimeDTO.setKey(PCP_SCALE_TO_MAX_TIME);
        pcpScaleToMaxTimeDTO.setValue("500");
        propertiesDTOs.add(pcpScaleToMaxTimeDTO);

        Map<String, Object> propertyMap = new HashMap<>(){{
            put(PCP_CORE_POOL_SIZE, 15);
            put(PCP_MAX_POOL_SIZE, 25);
            put(PCP_SCALE_TO_MAX_TIME, 500);
        }};

        ObjectMapper mapper = new ObjectMapper();

        MvcResult mvcResult = this.mockMvc.perform(
                put(API_PREFIX + ADMIN_PREFIX + PROPERTIES_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(propertiesDTOs))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(200, mvcResult.getResponse().getStatus());

        String result = mvcResult.getResponse().getContentAsString();
        List<PropertiesDTO> propertiesDTOsRetrieved = mapper.readValue(result, new TypeReference<List<PropertiesDTO>>() { } );

        Assert.assertEquals(3, propertiesDTOsRetrieved.size());
        for(PropertiesDTO propertiesDTO : propertiesDTOsRetrieved) {
            Object value = propertyMap.get(propertiesDTO.getKey());

            Assert.assertNotNull(value);
            Assert.assertEquals(value.toString(), propertiesDTO.getValue());
        }
    }
}
