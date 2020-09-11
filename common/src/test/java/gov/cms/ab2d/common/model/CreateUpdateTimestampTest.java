package gov.cms.ab2d.common.model;

import gov.cms.ab2d.common.repository.BeneficiaryRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SpringBootTest
@Testcontainers
@TestPropertySource(locations = "/application.common.properties")
public class CreateUpdateTimestampTest {

    private static final String PATIENT_ID_STR = "Timestamp Test Beneficiary";
    private static final String PATIENT_ID_STR_TOO = "Timestamp Test Beneficiary 2";


    @Autowired
    BeneficiaryRepository beneficiaryRepository;

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Test
    public void testTimestamps() {
        Beneficiary bene = new Beneficiary();
        bene.setPatientId(PATIENT_ID_STR);
        assertNull(bene.getId());
        assertNull(bene.getCreated());
        assertNull(bene.getModified());

        Beneficiary savedBeneficiary = beneficiaryRepository.save(bene);
        assertEquals(PATIENT_ID_STR, savedBeneficiary.getPatientId());
        assertNotNull(savedBeneficiary.getId());
        assertNotNull(savedBeneficiary.getCreated());
        assertNotNull(savedBeneficiary.getModified());

        LocalDateTime created = savedBeneficiary.getCreated();
        LocalDateTime modified = savedBeneficiary.getModified();
        savedBeneficiary.setPatientId(PATIENT_ID_STR_TOO);
        Beneficiary finaleBeneficiary = beneficiaryRepository.save(savedBeneficiary);
        assertEquals(created, finaleBeneficiary.getCreated());
        assertNotEquals(modified, finaleBeneficiary.getModified());
    }

    @SuppressWarnings("unused")
    @AfterEach
    private void tearDown() {
        beneficiaryRepository.deleteAll();
    }
}
