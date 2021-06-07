package gov.cms.ab2d.patientendpoint;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.fhir.IdentifierUtils;
import gov.cms.ab2d.fhir.PatientIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
public class CommandLineRunnerTest {
    @Autowired
    private BFDClient bfdClient;

    private static FhirVersion version;
    private static String contract;
    private static Integer month;
    private static Integer year;
    private static long totalTime;
    private static long totalBenes;

    @BeforeAll
    static void initEnvironment() {
        version = FhirVersion.valueOf(System.getenv("TEST_VERSION"));
        contract = System.getenv("TEST_CONTRACT");
        month = Integer.valueOf(System.getenv("TEST_MONTH"));
        year = null;
        try {
            year = Integer.valueOf(System.getenv("TEST_YEAR"));
        } catch (Exception ex) {
            System.out.println("No year specified");
        }
    }

    @Test
    public void testEndpoint() {
        testEndpointFull(true);
    }

    @Test
    public void testEndpointSummary() {
        testEndpointFull(false);
    }

    public void testEndpointFull(boolean detailed) {
        totalTime = 0;
        totalBenes = 0;
        IBaseBundle bundle;
        if (year != null) {
            bundle = retrieveFromYearEndpoint(version, contract, month, year, detailed);
            while (BundleUtils.getNextLink(bundle) != null) {
                bundle = retrieveNextMonthEndpoint(version, bundle, detailed);
            }
        } else {
            bundle = retrieveFromMonthEndpoint(version, contract, month, detailed);
            while (BundleUtils.getNextLink(bundle) != null) {
                bundle = retrieveNextMonthEndpoint(version, bundle, detailed);
            }
        }
        System.out.println("\nTotal Benes: " + totalBenes);
        System.out.print("Total ");
        showTime(totalTime);
    }

    private IBaseBundle retrieveFromMonthEndpoint(FhirVersion version, String contractId, int month, boolean detailed) {
        long t1 = System.currentTimeMillis();
        System.out.println("Search for patients for " + version.toString() + " contract " + contractId + " month " + month);
        IBaseBundle bundle = bfdClient.requestPartDEnrolleesFromServer(version, contractId, month);
        long t2 = System.currentTimeMillis();
        totalTime += (t2 - t1);
        showTime(t1, t2);
        extractIds(bundle, version, detailed);
        return bundle;
    }

    private IBaseBundle retrieveNextMonthEndpoint(FhirVersion version, IBaseBundle bundle, boolean detailed) {
        long t1 = System.currentTimeMillis();
        System.out.println("Search for next patients");
        IBaseBundle nextBundle = bfdClient.requestNextBundleFromServer(version, bundle);
        long t2 = System.currentTimeMillis();
        totalTime += (t2 - t1);
        showTime(t1, t2);
        extractIds(nextBundle, version, detailed);
        return nextBundle;
    }

    private void showTime(long t1, long t2) {
        long timeDiff = t2 - t1;
        showTime(timeDiff);
    }

    private void showTime(long timeDiff) {
        if (timeDiff < 10000) {
            System.out.println("Time: " + timeDiff + " ms");
        } else if (timeDiff < (60000)) {
            System.out.println("Time: " + (timeDiff/1000) + " sec");
        } else {
            System.out.println("Time: " + (timeDiff/60000) + " min");
        }
    }

    private void extractIds(IBaseBundle bundle, FhirVersion version, boolean detailed) {
        List<IDomainResource> patients = BundleUtils.getPatientStream(bundle, version).collect(Collectors.toList());
        System.out.println(patients.size() + " beneficiaries");
        totalBenes += patients.size();
        if (detailed) {
            for (IDomainResource patient : patients) {
                List<PatientIdentifier> ids = IdentifierUtils.getIdentifiers(patient);
                PatientIdentifier beneId = IdentifierUtils.getBeneId(ids);
                System.out.println("Bene: " + beneId.getValue());
                ids.forEach(id -> System.out.println("    Type: " + id.getType() + " (" + id.getCurrency().toString() + ") " + id.getValue()));
            }
        }
    }

    private IBaseBundle retrieveFromYearEndpoint(FhirVersion version, String contractId, int month, Integer year, boolean detailed) {
        long t1 = System.currentTimeMillis();
        System.out.println("Search for patients for " + version.toString() + " contract " + contractId + " month " + month + "/" + year);
        IBaseBundle bundle = bfdClient.requestPartDEnrolleesFromServer(version, contractId, month, year);
        long t2 = System.currentTimeMillis();
        totalTime += (t2 - t1);
        showTime(t1, t2);
        extractIds(bundle, version, detailed);
        return bundle;

    }
}
