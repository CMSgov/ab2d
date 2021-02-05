package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Identifiers;
import gov.cms.ab2d.fhir.IdentifierUtils;

import java.util.*;

import static gov.cms.ab2d.worker.processor.coverage.CoverageMappingCallable.*;

public class BundleUtils {

    public static Identifiers createIdentifierWithoutMbi(String beneficiaryId) {
        return new Identifiers(beneficiaryId, null, new LinkedHashSet<>());
    }

    public static Identifiers createIdentifier(String beneficiaryId, String currentMbi, String... historicMbis) {
        return new Identifiers(beneficiaryId, currentMbi, new LinkedHashSet<>(Set.of(historicMbis)));
    }

    public static org.hl7.fhir.dstu3.model.Bundle createBundle(org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent ... bundleEntries) {
        var bundle = new org.hl7.fhir.dstu3.model.Bundle();
        var entries = bundle.getEntry();
        for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent e : bundleEntries) {
            entries.add(e);
        }
        return bundle;
    }

    public static org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent createBundleEntry(String patientId, String mbi, int year) {
        var component = new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent();
        component.setResource(createPatient(patientId, mbi, year));
        return component;
    }

    public static org.hl7.fhir.dstu3.model.Patient createPatient(String patientId, String mbi, int year) {
        return createPatient(patientId, mbi, year, true);
    }

    public static org.hl7.fhir.dstu3.model.Patient createPatientWithMultipleMbis(String patientId, int numMbis, int year) {
        var patient = new org.hl7.fhir.dstu3.model.Patient();
        patient.getIdentifier().add(createBeneficiaryIdentifier(patientId));

        patient.getIdentifier().add(createMbiIdentifier(patientId + "mbi-" + 0, true));

        for (int idx = 1; idx < numMbis; idx++) {
            patient.getIdentifier().add(createMbiIdentifier(patientId + "mbi-" + idx, false));
        }

        patient.getExtension().add(createReferenceYearExtension(year));
        return patient;
    }

    private static org.hl7.fhir.dstu3.model.Patient createPatient(String patientId, String mbi, int year, boolean current) {
        var patient = new org.hl7.fhir.dstu3.model.Patient();
        patient.getIdentifier().add(createBeneficiaryIdentifier(patientId));
        patient.getIdentifier().add(createMbiIdentifier(mbi, current));
        patient.getExtension().add(createReferenceYearExtension(year));
        return patient;
    }

    public static org.hl7.fhir.dstu3.model.Identifier createBeneficiaryIdentifier(String beneficiaryId) {
        var identifier = new org.hl7.fhir.dstu3.model.Identifier();
        identifier.setSystem(IdentifierUtils.BENEFICIARY_ID);
        identifier.setValue(beneficiaryId);
        return identifier;
    }

    public static org.hl7.fhir.dstu3.model.Identifier createMbiIdentifier(String mbi, boolean current) {
        var identifier = new org.hl7.fhir.dstu3.model.Identifier();
        identifier.setSystem(MBI_ID);
        identifier.setValue(mbi);

        org.hl7.fhir.dstu3.model.Coding coding = new org.hl7.fhir.dstu3.model.Coding();

        if (current) {
            coding.setCode(CURRENT_MBI);
        } else {
            coding.setCode(HISTORIC_MBI);
        }

        org.hl7.fhir.dstu3.model.Extension extension = new org.hl7.fhir.dstu3.model.Extension();
        extension.setUrl(CURRENCY_IDENTIFIER);
        extension.setValue(coding);
        identifier.setExtension(Collections.singletonList(extension));

        return identifier;
    }

    public static org.hl7.fhir.dstu3.model.Extension createReferenceYearExtension(int year) {
        org.hl7.fhir.dstu3.model.DateType dateType = new org.hl7.fhir.dstu3.model.DateType();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, 1, 1, 0, 0, 0);
        dateType.setValue(calendar.getTime());

        return new org.hl7.fhir.dstu3.model.Extension()
                .setUrl("https://bluebutton.cms.gov/resources/variables/rfrnc_yr")
                .setValue(dateType);
    }
}
