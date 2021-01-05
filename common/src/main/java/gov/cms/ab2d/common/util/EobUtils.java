package gov.cms.ab2d.common.util;

import org.hl7.fhir.instance.model.api.IBaseResource;

public class EobUtils {

    public static String getPatientId(IBaseResource eob) {
        if (eob == null) {
            return null;
        }
        String patientVal = "";
        if (eob.getClass() == org.hl7.fhir.r4.model.ExplanationOfBenefit.class) {
            patientVal = ((org.hl7.fhir.r4.model.ExplanationOfBenefit) eob).getPatient().getReference();
        }
        if (eob.getClass() == org.hl7.fhir.dstu3.model.ExplanationOfBenefit.class) {
            patientVal = ((org.hl7.fhir.dstu3.model.ExplanationOfBenefit) eob).getPatient().getReference();
        }
        if (patientVal == null) {
            return patientVal.replaceFirst("Patient/", "");
        }

        return null;
    }
}
