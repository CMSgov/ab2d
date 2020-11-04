package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import org.hl7.fhir.dstu3.model.*;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class BundleUtils {

    public static final String BENEFICIARY_ID = "https://bluebutton.cms.gov/resources/variables/bene_id";
    public static final String MBI_ID = "http://hl7.org/fhir/sid/us-mbi";

    public static Bundle createBundle(Bundle.BundleEntryComponent ... bundleEntries) {
        var bundle = new Bundle();
        var entries = bundle.getEntry();
        for (Bundle.BundleEntryComponent e : bundleEntries) {
            entries.add(e);
        }
        return bundle;
    }

    public static List<ContractBeneficiaries.PatientDTO> getPatient(String id, Collection<ContractBeneficiaries.PatientDTO> patients) {
        return patients.stream().filter(c -> c.getPatientId().equalsIgnoreCase(id)).collect(Collectors.toList());
    }

    public static Bundle.BundleEntryComponent createBundleEntry(String patientId, String mbi, int year) {
        var component = new Bundle.BundleEntryComponent();
        component.setResource(createPatient(patientId, mbi, year));
        return component;
    }

    public static Patient createPatient(String patientId, String mbi, int year) {
        var patient = new Patient();
        patient.getIdentifier().add(createBeneficiaryIdentifier(patientId));
        patient.getIdentifier().add(createMbiIdentifier(mbi));
        patient.getExtension().add(createReferenceYearExtension(year));
        return patient;
    }

    public static Identifier createBeneficiaryIdentifier(String beneficiaryId) {
        var identifier = new Identifier();
        identifier.setSystem(BENEFICIARY_ID);
        identifier.setValue(beneficiaryId);
        return identifier;
    }

    public static Identifier createMbiIdentifier(String mbi) {
        var identifier = new Identifier();
        identifier.setSystem(MBI_ID);
        identifier.setValue(mbi);
        return identifier;
    }

    public static Extension createReferenceYearExtension(int year) {
        DateType dateType = new DateType();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, 1, 1, 0, 0, 0);
        dateType.setValue(calendar.getTime());

        return new Extension()
                .setUrl("https://bluebutton.cms.gov/resources/variables/rfrnc_yr")
                .setValue(dateType);
    }
}
