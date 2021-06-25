package gov.cms.ab2d.patientendpoint;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.fhir.IdentifierUtils;
import gov.cms.ab2d.fhir.PatientIdentifier;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gov.cms.ab2d.fhir.FhirVersion.R4;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
@Slf4j
public class PatientEndpointTests {
    @Autowired
    private BFDClient client;

    @ParameterizedTest
    @MethodSource("getVersion")
    public void testPatientEndpoint(FhirVersion version, String contract, int month, int year) {
        BFDClient.BFD_BULK_JOB_ID.set("TEST");

        log.info("Testing IDs for " + version.toString());
        List<PatientIdentifier> patientIds = new ArrayList<>();

        log.info(String.format("Do Request for %s for %02d/%04d", contract, month, year));
        IBaseBundle bundle = client.requestPartDEnrolleesFromServer(version, contract, month, year);
        assertNotNull(bundle);
        int numberOfBenes = BundleUtils.getEntries(bundle).size();
        patientIds.addAll(extractIds(bundle, version));
        log.info("Found: " + numberOfBenes + " benes");

        while (BundleUtils.getNextLink(bundle) != null) {
            log.info(String.format("Do Next Request for %s for %02d/%04d", contract, month, year));
            bundle = client.requestNextBundleFromServer(version, bundle);
            numberOfBenes += BundleUtils.getEntries(bundle).size();
            log.info("Found: " + numberOfBenes + " benes");
            patientIds.addAll(extractIds(bundle, version));
        }

        log.info("Contract: " + contract + " has " + numberOfBenes + " benes with " + patientIds.size() + " ids");
        assertTrue(patientIds.size() >= 1000);
        assertEquals(0, patientIds.size() % 1000);
        assertTrue(patientIds.size() >= (2 * numberOfBenes));
    }

    public static List<PatientIdentifier> extractIds(IBaseBundle bundle, FhirVersion version) {
        List<PatientIdentifier> ids = new ArrayList<>();
        List patients = BundleUtils.getPatientStream(bundle, version)
                .collect(Collectors.toList());
        patients.forEach(c -> ids.addAll(IdentifierUtils.getIdentifiers((IDomainResource) c)));
        return ids;
    }

    /**
     * Return the different versions of FHIR to test against
     *
     * @return the stream of FHIR versions
     */
    static Stream<Arguments> getVersion() {
        if (v2Enabled()) {
            return Stream.of(arguments(STU3, "Z0001", 1, 3), arguments(R4, "Z0001", 1, 3));
        } else {
            return Stream.of(arguments(STU3, "Z0001", 1, 3));
        }
    }

    private static boolean v2Enabled() {
        String v2Enabled = System.getenv("AB2D_V2_ENABLED");
        if (v2Enabled != null && v2Enabled.equalsIgnoreCase("true")) {
            return true;
        }
        return false;
    }
}
