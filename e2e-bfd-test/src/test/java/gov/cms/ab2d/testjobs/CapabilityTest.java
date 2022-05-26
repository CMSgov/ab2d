package gov.cms.ab2d.testjobs;

import com.amazonaws.services.sns.AmazonSNS;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.worker.sns.ProgressUpdater;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Testcontainers
public class CapabilityTest {
    @Autowired
    private BFDClient bfdClient;

    // Disable SNS
    @MockBean
    AmazonSNS amazonSns;

    @MockBean
    private ProgressUpdater progressUpdater;

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
