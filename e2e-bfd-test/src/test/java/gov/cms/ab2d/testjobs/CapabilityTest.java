package gov.cms.ab2d.testjobs;

import gov.cms.ab2d.AB2DLocalstackContainer;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.fhir.FhirVersion;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class CapabilityTest {
    @Autowired
    private BFDClient bfdClient;

    @Container
    private static final AB2DLocalstackContainer LOCALSTACK_CONTAINER = new AB2DLocalstackContainer();

    @Container
    private static final PostgreSQLContainer postgres = new AB2DPostgresqlContainer();

    @Test
    void testCapabilityStatement() {
        IBaseConformance capabilityStatement = null;
        try {
            capabilityStatement = bfdClient.capabilityStatement(FhirVersion.STU3);
        } catch (Exception e) {
            fail("Exception occurred while trying to retrieve capability statement", e);
        }
    }
}
