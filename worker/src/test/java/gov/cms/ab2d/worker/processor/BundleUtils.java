package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import org.hl7.fhir.dstu3.model.*;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class BundleUtils {
    public static final String BENEFICIARY_ID = "https://bluebutton.cms.gov/resources/variables/bene_id";
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

    public static Bundle.BundleEntryComponent createBundleEntry(String patientId, int year) {
        var component = new Bundle.BundleEntryComponent();
        component.setResource(createPatient(patientId, year));
        return component;
    }

    public static Patient createPatient(String patientId, int year) {
        var patient = new Patient();
        patient.getIdentifier().add(createIdentifier(patientId));
        patient.getExtension().add(createReferenceYearExtension(year));
        return patient;
    }

    public static Identifier createIdentifier(String patientId) {
        var identifier = new Identifier();
        identifier.setSystem(BENEFICIARY_ID);
        identifier.setValue(patientId);
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
