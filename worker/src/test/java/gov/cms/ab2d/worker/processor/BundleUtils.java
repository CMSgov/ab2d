package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;

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

    public static List<ContractBeneficiaries.PatientDTO> getPatient(String id, List<ContractBeneficiaries.PatientDTO> patients) {
        return patients.stream().filter(c -> c.getPatientId().equalsIgnoreCase(id)).collect(Collectors.toList());
    }

    public static Bundle.BundleEntryComponent createBundleEntry(String patientId) {
        var component = new Bundle.BundleEntryComponent();
        component.setResource(createPatient(patientId));
        return component;
    }

    public static Patient createPatient(String patientId) {
        var patient = new Patient();
        patient.getIdentifier().add(createIdentifier(patientId));
        return patient;
    }

    public static Identifier createIdentifier(String patientId) {
        var identifier = new Identifier();
        identifier.setSystem(BENEFICIARY_ID);
        identifier.setValue(patientId);
        return identifier;
    }
}
