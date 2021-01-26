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
        try {
            IBase ref = (IBase) Versions.invokeGetMethod(eob, "getPatient");
            String patientVal =  (String) Versions.invokeGetMethod(ref, "getReference");
            if (patientVal != null) {
                return patientVal.replaceFirst("Patient/", "");
            }
        } catch (Exception ex) {
            log.error("Unable to find patient ID from resource");
            return null;
        }
        return null;
    }

    public static Object getPeriod(IBaseResource ben) {
        if (ben == null) {
            return null;
        }
        try {
            return Versions.invokeGetMethod(ben, "getBillablePeriod");
        } catch (Exception ex) {
            log.error("Cannot call getBillablePeriod form EOB");
            return null;
        }
    }
    public static Date getStartDate(IBaseResource ben) {
        Object period = getPeriod(ben);
        if (period == null) {
            return null;
        }
        try {
            return (Date) Versions.invokeGetMethod(period, "getStart");
        } catch (Exception ex) {
            log.error("Cannot call getBillablePeriod form EOB");
            return null;
        }
    }

    public static Date getEndDate(IBaseResource ben) {
        Object period = getPeriod(ben);
        if (period == null) {
            return null;
        }
        try {
            return (Date) Versions.invokeGetMethod(period, "getEnd");
        } catch (Exception ex) {
            log.error("Cannot call getBillablePeriod form EOB");
            return null;
        }
    }

    public static boolean isPartD(IBaseResource eob) {
        try {
            if (!eob.fhirType().endsWith("ExplanationOfBenefit")) {
                return false;
            }
            Object c = Versions.invokeGetMethod(eob, "getType");
            List codes = (List) Versions.invokeGetMethod(c, "getCoding");
            return codes.stream()
                    .filter(code -> {
                        try {
                            return ((String) Versions.invokeGetMethod(code, "getSystem")).endsWith(EOB_TYPE_CODE_SYS);
                        } catch (Exception e) {
                            log.error("Can't get coding system to determine part d");
                            return false;
                        }
                    })
                    .anyMatch(code -> {
                        try {
                            return ((String) Versions.invokeGetMethod(code, "getCode")).equalsIgnoreCase(EOB_TYPE_PART_D_CODE_VAL);
                        } catch (Exception e) {
                            log.error("Unable to get coding code value");
                            return false;
                        }
                    });
        } catch (Exception ex) {
            log.error("Unable to get the eob type");
            return false;
        }
    }
}
