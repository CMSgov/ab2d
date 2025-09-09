package gov.cms.ab2d.contracts.controller;

import gov.cms.ab2d.contracts.SpringBootApp;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.repository.ContractRepository;
import gov.cms.ab2d.contracts.util.AB2DPostgresqlContainer;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class ContractControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContractRepository contractRepository;

    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();

    @BeforeEach
    void cleanUp() {
        contractRepository.deleteAll();
    }

    @AfterEach
    void cleanUpAfter() {
        contractRepository.deleteAll();
    }

    @Test
    void testList() throws Exception {

        this.mockMvc.perform(get("/contracts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        Contract originalContract = new Contract("Z0000", "test", 42l, "ORG", "Marketing", 100, 95);
        originalContract.setAttestedOn(OffsetDateTime.now());
        originalContract = contractRepository.save(originalContract);

        this.mockMvc.perform(get("/contracts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].contractNumber").value("Z0000"))
                .andExpect(jsonPath("$.[0].contractName").value("test"));

        this.mockMvc.perform(put("/contracts")
                        .content("{\n" +
                                "    \"created\": \"2022-11-21T10:25:02.851161-08:00\",\n" +
                                "    \"modified\": \"2022-12-01T07:20:33.040608-08:00\",\n" +
                                "    \"id\": " + originalContract.getId() +",\n" +
                                "    \"contractNumber\": \"Z0000\",\n" +
                                "    \"contractName\": \"Z0001\",\n" +
                                "    \"contractType\": \"NORMAL\",\n" +
                                "    \"attestedOn\": \"2020-03-01T00:00:00-08:00\",\n" +
                                "    \"testContract\": true,\n" +
                                "    \"estattestationTime\": \"2020-03-01T03:00:00-05:00\",\n" +
                                "    \"autoUpdatable\": false\n" +
                                "}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        this.mockMvc.perform(get("/contracts")
                        .param("contractId", originalContract.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].contractNumber").value("Z0000"))
                .andExpect(jsonPath("$.[0].contractName").value("Z0001"));

        this.mockMvc.perform(get("/contracts/Z0000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractNumber").value("Z0000"))
                .andExpect(jsonPath("$.id").value(originalContract.getId()))
                .andExpect(jsonPath("$.contractName").value("Z0001"));

        this.mockMvc.perform(get("/contracts/NotARealNumber")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());

        this.mockMvc.perform(get("/contracts")
                        .param("contractId", "01010100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }
}
