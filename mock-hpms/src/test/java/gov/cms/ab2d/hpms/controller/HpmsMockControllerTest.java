package gov.cms.ab2d.hpms.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class HpmsMockControllerTest {

    @Autowired
    HpmsMockController hpmsMockController;

    @Test
    public void testOrganization() throws IOException {
        ResponseEntity<String> response = hpmsMockController.getOrganizationInfo();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("ABC Marketing"));
    }

    @Test
    public void testAttestationError() throws IOException {
        List<String> contractIds = new ArrayList<>(List.of("S1234", "S2341", "S9876"));
        ResponseEntity<String> response = hpmsMockController.getAttestation(new JsonStringArray(contractIds));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("Missing or invalid contractIds"));
    }

    @Test
    public void testAttestationSuccess() throws IOException {
        List<String> contractIds = new ArrayList<>(List.of("S1234", "S2341"));
        ResponseEntity<String> response = hpmsMockController.getAttestation(new JsonStringArray(contractIds));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("\"attestationDate\": \"2/13/2020\""));
    }

}
