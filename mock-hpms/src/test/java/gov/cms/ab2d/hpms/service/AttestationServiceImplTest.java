package gov.cms.ab2d.hpms.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class AttestationServiceImplTest {

    @Autowired
    AttestationService attestationService;

    @Test
    public void test2known() {
        List<String> contractIds = new ArrayList<>(List.of("S1234", "S2341"));
        List<String> results = attestationService.retrieveAttestations(contractIds);
        assertEquals(2, results.size());
        assertTrue(results.get(0).contains("\"attestationDate\": \"01/31/2020\""));
        assertTrue(results.get(1).contains("\"attestationDate\": \"02/13/2020\""));
    }

    @Test
    public void testNotFound() {
        List<String> contractIds = new ArrayList<>(List.of("S1234", "S2341", "S6666"));
        assertTrue(attestationService.retrieveAttestations(contractIds).isEmpty());
    }
}
