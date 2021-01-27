package gov.cms.ab2d.fhir;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.Date;
import java.util.List;

@Slf4j
public class EobUtils {
    public static final String EOB_TYPE_CODE_SYS = "eob-type";
    public static final String EOB_TYPE_PART_D_CODE_VAL = "PDE";

    public static String getPatientId(IBase eob) {
        if (eob == null) {
            return null;
        }
        IBase ref = (IBase) Versions.invokeGetMethod(eob, "getPatient");
        String patientVal =  (String) Versions.invokeGetMethod(ref, "getReference");
        if (patientVal != null) {
            return patientVal.replaceFirst("Patient/", "");
        }
        return null;
    }

    public static Object getPeriod(IBaseResource ben) {
        if (ben == null) {
            return null;
        }
        return Versions.invokeGetMethod(ben, "getBillablePeriod");
    }

    public static Date getStartDate(IBaseResource ben) {
        Object period = getPeriod(ben);
        if (period == null) {
            return null;
        }
        return (Date) Versions.invokeGetMethod(period, "getStart");
    }

    public static Date getEndDate(IBaseResource ben) {
        Object period = getPeriod(ben);
        if (period == null) {
            return null;
        }
        return (Date) Versions.invokeGetMethod(period, "getEnd");
    }

    public static boolean isPartD(IBaseResource eob) {
        if (eob == null) {
            return false;
        }
        if (!eob.fhirType().endsWith("ExplanationOfBenefit")) {
            return false;
        }
        Object c = Versions.invokeGetMethod(eob, "getType");
        List codes = (List) Versions.invokeGetMethod(c, "getCoding");
        return codes.stream()
                .filter(code -> ((String) Versions.invokeGetMethod(code, "getSystem")).endsWith(EOB_TYPE_CODE_SYS))
                .anyMatch(code -> EOB_TYPE_PART_D_CODE_VAL.equalsIgnoreCase((String) Versions.invokeGetMethod(code, "getCode")));
    }
}
