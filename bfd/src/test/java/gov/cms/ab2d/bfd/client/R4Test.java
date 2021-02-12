package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.fhir.MetaDataUtils;
import gov.cms.ab2d.fhir.Versions;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.fail;

/**
 * Class used to test against the real R4 BFD server (you need to change values in properties file to get it to work)
 */
@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.bfd.properties")
public class R4Test {
    @Autowired BFDClient client;

    // @Test
    void testBasicTest() {
        IBaseConformance capabilityStatement = client.capabilityStatement(Versions.FhirVersions.R4);
        if (!MetaDataUtils.metaDataValid(capabilityStatement, Versions.FhirVersions.R4)) {
            fail("Invalid meta data");
        }
        IParser parser = getJsonParser(FhirContext.forR4());
        String val = parser.encodeResourceToString(capabilityStatement);
        System.out.println(val);
    }

    // @Test
    void testPatient() {
        Set<String> patients = new HashSet<>();
        IBaseBundle bundle = client.requestPartDEnrolleesFromServer(Versions.FhirVersions.STU3, "Z0002", 12);
        IParser parser = getJsonParser(FhirContext.forR4());
        String val = parser.encodeResourceToString(bundle);
        System.out.println(val);
    }

    // @Test
    void testPatientByMBI() {
        Bundle response = (Bundle) client.requestPatientByMBI(Versions.FhirVersions.R4, "3S17H00AA00");
        IParser parser = getJsonParser(FhirContext.forR4());
        String val = parser.encodeResourceToString(response);
        System.out.println(val);
    }

    // @Test
    void testPatientByMBI3() {
        IBaseBundle b = client.requestEOBFromServer(Versions.FhirVersions.R4, "999999999999999999999999");
        //org.hl7.fhir.dstu3.model.Bundle response1 = (org.hl7.fhir.dstu3.model.Bundle) client.requestPartDEnrolleesFromServer(Versions.FhirVersions.STU3, "Z0001", 12);
        //org.hl7.fhir.dstu3.model.Patient p = ((org.hl7.fhir.dstu3.model.Patient) (response1.getEntry().get(0).getResource()));
        //org.hl7.fhir.dstu3.model.Identifier i = p.getIdentifier().stream()
        //        .filter(c -> c.getSystem().contains("us-mbi"))
        //        .findFirst().orElse(null);
        org.hl7.fhir.dstu3.model.Bundle response2 = (org.hl7.fhir.dstu3.model.Bundle) client.requestPatientByMBI(Versions.FhirVersions.STU3, "2SC0A00AA00");
        // IParser parser = getJsonParser(FhirContext.forR4());
        System.out.println("1");
    }

    // @Test
    void testPatientSearch() {
        Set<String> patients = new HashSet<>();
        IBaseBundle bundle = client.requestPartDEnrolleesFromServer(Versions.FhirVersions.R4, "Z0012", 12);
        Bundle r4bundle = (Bundle) bundle;
        for (Bundle.BundleEntryComponent c : r4bundle.getEntry()) {
            Patient p = (Patient) c.getResource();
            for (Identifier id : p.getIdentifier()) {
                if (id.getSystem().equalsIgnoreCase("https://bluebutton.cms.gov/resources/variables/bene_id")) {
                    String idS = id.getValue();
                    if (!patients.contains(idS)) {
                        System.out.println("Patient: " + idS);
                        patients.add(idS);
                    }
                }
            }
        }
        while (BundleUtils.getNextLink(r4bundle) != null) {
            System.out.println("Next bundle");
            try {
                bundle = client.requestNextBundleFromServer(Versions.FhirVersions.R4, r4bundle);
                r4bundle = (Bundle) bundle;
                for (Bundle.BundleEntryComponent c : r4bundle.getEntry()) {
                    Patient p = (Patient) c.getResource();
                    for (Identifier id : p.getIdentifier()) {
                        if (id.getSystem().equalsIgnoreCase("https://bluebutton.cms.gov/resources/variables/bene_id")) {
                            String idS = id.getValue();
                            if (!patients.contains(idS)) {
                                System.out.println("Patient: " + idS);
                                patients.add(idS);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.println("Unable to get next bundle: " + ex.getMessage());
                break;
            }
        }
        System.out.println(patients.size() + " patients");
        for (String p : patients) {
            try {
                Bundle bundle1 = (Bundle) client.requestEOBFromServer(Versions.FhirVersions.R4, p);
                if (bundle1.getTotal() > 0) {
                    System.out.println("*************** Found " + bundle1.getTotal() + " for " + p + " ****************");
                    Bundle bundle2 = (Bundle) client.requestEOBFromServer(Versions.FhirVersions.R4, p,
                            OffsetDateTime.of(2020, 7, 1, 0, 0, 0, 0, ZoneOffset.UTC));
                    printOutUpdateDate(bundle2);
                    while (BundleUtils.getNextLink(bundle2) != null) {
                        bundle2 = (Bundle) client.requestNextBundleFromServer(Versions.FhirVersions.R4, bundle2);
                        printOutUpdateDate(bundle2);
                    }
                }
            } catch (Exception ex) {
                System.out.println("Unable to get EOBs for " + p + " - " + ex.getMessage());
            }
        }
    }

    // @Test
    void testEobSearch() {
        // This one has data
        Bundle bundle1 = (Bundle) client.requestEOBFromServer(Versions.FhirVersions.R4, "-20140000010000");
        System.out.println(bundle1.getTotal() + " is total ");
        FhirContext r4Context = FhirContext.forR4();
        IParser parser = getJsonParser(r4Context);
        String val = parser.encodeResourceToString(bundle1);
        System.out.println(val);
    }

    // @Test
    void testEobSearchAll() {
        int totalNumber = 0;
        // This one has data
        Bundle bundle1 = (Bundle) client.requestEOBFromServer(Versions.FhirVersions.R4, "-20140000010000",
                OffsetDateTime.of(2020, 7, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        totalNumber += bundle1.getEntry().size();
        printOutUpdateDate(bundle1);
        while (BundleUtils.getNextLink(bundle1) != null) {
            bundle1 = (Bundle) client.requestNextBundleFromServer(Versions.FhirVersions.R4, bundle1);
            totalNumber += bundle1.getEntry().size();
            printOutUpdateDate(bundle1);
        }
        System.out.println("Total found: " + totalNumber);
    }

    static void printOutUpdateDate(Bundle bundle) {
        for (Bundle.BundleEntryComponent c : bundle.getEntry()) {
            ExplanationOfBenefit eob = (ExplanationOfBenefit) c.getResource();
            if (eob.getBillablePeriod() != null && eob.getBillablePeriod().getStart() != null) {
                System.out.println(eob.getMeta().getLastUpdatedElement() + ", " + eob.getBillablePeriod().getStart() + " - " + eob.getBillablePeriod().getEnd());
            }
        }
    }

    static IParser getJsonParser(FhirContext context) {
        EncodingEnum respType = EncodingEnum.forContentType(EncodingEnum.JSON_PLAIN_STRING);
        return respType.newParser(context);
    }
}
