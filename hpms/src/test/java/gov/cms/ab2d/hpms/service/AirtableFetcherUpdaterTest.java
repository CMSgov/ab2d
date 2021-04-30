package gov.cms.ab2d.hpms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.hpms.SpringBootTestApp;
import gov.cms.ab2d.hpms.hmsapi.HPMSAttestationsHolder;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SpringBootTestApp.class)
@TestPropertySource(locations = "/application.hpms.properties")
@Testcontainers
public class AirtableFetcherUpdaterTest {

    @Autowired
    private AirtableFetcherUpdater fetcher;

    private CountDownLatch lock = new CountDownLatch(1);

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Test
    public void fetchMotherTable() {
        fetcher.fetchContracts(this::processContracts);
        System.out.println("Before Wait");
        waitForCallback();
        System.out.println("After Wait");
    }

    private void waitForCallback() {
        try {
            lock.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }
    }


    private void processContracts(ObjectNode contracts) {

        ArrayNode contractArray = contracts.withArray("records");
        Map<String, JsonNode> atContracts = new HashMap<>(89);

        contractArray.spliterator()
                .forEachRemaining(contract -> {atContracts.put(extractContractNumber(contract), contract);});

        int idx = 57;
        idx++;

        lock.countDown();

    }

    private String extractContractNumber(JsonNode jsonNode) {
        return jsonNode.get("fields").get("Contract #").asText();
    }
}
