package gov.cms.ab2d.common.util;

import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.model.Identifiers;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.List;
import java.util.Map;

public class ExtensionUtils {
    public static final String CURRENT_MBI = "current";
    public static final String HISTORIC_MBI = "historic";
    public static final String MBI_ID = "http://hl7.org/fhir/sid/us-mbi";
    public static final String CURRENCY_IDENTIFIER =
            "https://bluebutton.cms.gov/resources/codesystem/identifier-currency";
    static final String ID_EXT = "http://hl7.org/fhir/StructureDefinition/elementdefinition-identifier";

    public static void addMbiIdsToEobs(List<IBaseResource> eobs, Map<String, CoverageSummary> patients) {
        if (eobs == null || eobs.isEmpty()) {
            return;
        }
        // Get first EOB Bene ID
        IBaseResource eob = eobs.get(0);

        // Add extesions only if beneficiary id is present and known to memberships
        String benId = EobUtils.getPatientId(eob);
        if (benId != null && patients.containsKey(benId)) {
            Identifiers patient = patients.get(benId).getIdentifiers();

            // Add each mbi to each eob
            if (patient.getCurrentMbi() != null) {
                IBaseExtension currentMbiExtension = createExtension(eob, patient.getCurrentMbi(), true);
                eobs.forEach(e -> addExtension(e, currentMbiExtension));
            }

            for (String mbi : patient.getHistoricMbis()) {
                IBaseExtension mbiExtension = createExtension(eob, mbi, false);
                eobs.forEach(e -> addExtension(e, mbiExtension));
            }
        }
    }

    static void addExtension(IBaseResource resource, IBaseExtension extension) {
        if (resource.getClass() == org.hl7.fhir.r4.model.ExplanationOfBenefit.class) {
            ((org.hl7.fhir.r4.model.ExplanationOfBenefit) resource).addExtension((org.hl7.fhir.r4.model.Extension) extension);
        }
        if (resource.getClass() == org.hl7.fhir.dstu3.model.ExplanationOfBenefit.class) {
            ((org.hl7.fhir.dstu3.model.ExplanationOfBenefit) resource).addExtension((org.hl7.fhir.dstu3.model.Extension) extension);
        }
    }

    static IBaseExtension createExtension(IBaseResource resource, String mbi, boolean current) {
        if (resource.getClass() == org.hl7.fhir.r4.model.ExplanationOfBenefit.class) {
            org.hl7.fhir.r4.model.Identifier identifier = new org.hl7.fhir.r4.model.Identifier().setSystem(MBI_ID).setValue(mbi);

            org.hl7.fhir.r4.model.Coding coding = new org.hl7.fhir.r4.model.Coding()
                    .setCode(current ? CURRENT_MBI : HISTORIC_MBI);

            org.hl7.fhir.r4.model.Extension currencyExtension = new org.hl7.fhir.r4.model.Extension()
                    .setUrl(CURRENCY_IDENTIFIER)
                    .setValue(coding);
            identifier.setExtension(List.of(currencyExtension));

            return new org.hl7.fhir.r4.model.Extension().setUrl(ID_EXT).setValue(identifier);

        }
        if (resource.getClass() == org.hl7.fhir.dstu3.model.ExplanationOfBenefit.class) {
            org.hl7.fhir.dstu3.model.Identifier identifier = new org.hl7.fhir.dstu3.model.Identifier().setSystem(MBI_ID).setValue(mbi);

            org.hl7.fhir.dstu3.model.Coding coding = new org.hl7.fhir.dstu3.model.Coding()
                    .setCode(current ? CURRENT_MBI : HISTORIC_MBI);

            org.hl7.fhir.dstu3.model.Extension currencyExtension = new org.hl7.fhir.dstu3.model.Extension()
                    .setUrl(CURRENCY_IDENTIFIER)
                    .setValue(coding);
            identifier.setExtension(List.of(currencyExtension));

            return new org.hl7.fhir.dstu3.model.Extension().setUrl(ID_EXT).setValue(identifier);
        }
        return null;
    }
}
