package gov.cms.ab2d.worker.processor.eob;

import gov.cms.ab2d.common.model.Identifiers;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import org.hl7.fhir.dstu3.model.*;

import java.util.*;
import java.util.stream.Collectors;

import static gov.cms.ab2d.worker.processor.eob.PatientContractCallable.*;

public class BundleUtils {

    public static final String BENEFICIARY_ID = "https://bluebutton.cms.gov/resources/variables/bene_id";

    public static Identifiers createIdentifierWithoutMbi(String beneficiaryId) {
        return new Identifiers(beneficiaryId, null, new LinkedHashSet<>());
    }

    public static Identifiers createIdentifier(String beneficiaryId, String currentMbi, String... historicMbis) {
        return new Identifiers(beneficiaryId, currentMbi, new LinkedHashSet<>(Set.of(historicMbis)));
    }

    public static Bundle createBundle(Bundle.BundleEntryComponent ... bundleEntries) {
        var bundle = new Bundle();
        var entries = bundle.getEntry();
        for (Bundle.BundleEntryComponent e : bundleEntries) {
            entries.add(e);
        }
        return bundle;
    }

    public static List<ContractBeneficiaries.PatientDTO> getPatient(String id, Collection<ContractBeneficiaries.PatientDTO> patients) {
        return patients.stream().filter(c -> c.getBeneficiaryId().equalsIgnoreCase(id)).collect(Collectors.toList());
    }

    public static Bundle.BundleEntryComponent createBundleEntry(String patientId, String mbi, int year) {
        var component = new Bundle.BundleEntryComponent();
        component.setResource(createPatient(patientId, mbi, year));
        return component;
    }

    public static Patient createPatient(String patientId, String mbi, int year) {
        return createPatient(patientId, mbi, year, true);
    }

    public static Patient createPatientWithMultipleMbis(String patientId, int numMbis, int year) {
        var patient = new Patient();
        patient.getIdentifier().add(createBeneficiaryIdentifier(patientId));

        patient.getIdentifier().add(createMbiIdentifier(patientId + "mbi-" + 0, true));

        for (int idx = 1; idx < numMbis; idx++) {
            patient.getIdentifier().add(createMbiIdentifier(patientId + "mbi-" + idx, false));
        }

        patient.getExtension().add(createReferenceYearExtension(year));
        return patient;
    }

    private static Patient createPatient(String patientId, String mbi, int year, boolean current) {
        var patient = new Patient();
        patient.getIdentifier().add(createBeneficiaryIdentifier(patientId));
        patient.getIdentifier().add(createMbiIdentifier(mbi, current));
        patient.getExtension().add(createReferenceYearExtension(year));
        return patient;
    }

    public static Identifier createBeneficiaryIdentifier(String beneficiaryId) {
        var identifier = new Identifier();
        identifier.setSystem(BENEFICIARY_ID);
        identifier.setValue(beneficiaryId);
        return identifier;
    }

    public static Identifier createMbiIdentifier(String mbi, boolean current) {
        var identifier = new Identifier();
        identifier.setSystem(MBI_ID);
        identifier.setValue(mbi);

        Coding coding = new Coding();

        if (current) {
            coding.setCode(CURRENT_MBI);
        } else {
            coding.setCode(HISTORIC_MBI);
        }

        Extension extension = new Extension();
        extension.setUrl(CURRENCY_IDENTIFIER);
        extension.setValue(coding);
        identifier.setExtension(Collections.singletonList(extension));

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
