package gov.cms.ab2d.patientendpoint;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.fhir.IdentifierUtils;
import gov.cms.ab2d.fhir.PatientIdentifier;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
public class PatientEndpointTests {
    @Autowired
    private BFDClient client;

    @ParameterizedTest
    @MethodSource("getVersion")
    public void test(FhirVersion version, String contract, int month) {
        System.out.println("Testing IDs for " + version.toString());
        List<PatientIdentifier> patientIds = new ArrayList<>();

        System.out.println("Do Request for " + contract + " for " + month);
        IBaseBundle bundle = client.requestPartDEnrolleesFromServer(version, contract, 1);
        assertNotNull(bundle);
        int numberOfBenes = BundleUtils.getEntries(bundle).size();
        patientIds.addAll(extractIds(bundle, version));
        System.out.println("Found: " + numberOfBenes + " benes");

        while (BundleUtils.getNextLink(bundle) != null) {
            System.out.println("Do Next Request for " + contract + " for " + month);
            bundle = client.requestNextBundleFromServer(version, bundle);
            numberOfBenes += BundleUtils.getEntries(bundle).size();
            System.out.println("Found: " + numberOfBenes + " benes");
            patientIds.addAll(extractIds(bundle, version));
        }

        System.out.println("Contract: " + contract + " has " + numberOfBenes + " benes with " + patientIds.size() + " ids");
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
            return Stream.of(arguments(STU3, "Z0001", 1), arguments(R4, "Z0001", 1));
        } else {
            return Stream.of(arguments(STU3, "Z0001", 1));
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
