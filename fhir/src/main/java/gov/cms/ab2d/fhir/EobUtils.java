package gov.cms.ab2d.fhir;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.Date;
import java.util.List;

/**
 * Used to provide util methods to handle ExplanationOfBenefit objects
 */
@Slf4j
public final class EobUtils {
    public static final String EOB_TYPE_CODE_SYS = "eob-type";
    public static final String EOB_TYPE_PART_D_CODE_VAL = "PDE";

    private EobUtils() { }

    /**
     * Return the Patient ID from the EOB.getPatient().getReference & then strip off the "Patient/"
     *
     * @param eob - the ExplanationOfBenefit object
     * @return the patient ID from the patient reference
     */
    public static String getPatientId(IBase eob) {
        if (eob == null) {
            return null;
        }
        IBase ref = (IBase) Versions.invokeGetMethod(eob, "getPatient");
        return getJustId((String) Versions.invokeGetMethod(ref, "getReference"));
    }

    /**
     * Return just the ID portion of the Patient ID
     *
     * @param patientInfo - contains the full patient ID info in the format Patient/12344
     * @return just the ID portion - 12344
     */
    public static String getJustId(String patientInfo) {
        if (patientInfo != null) {
            return patientInfo.replaceFirst("Patient/", "");
        }
        return null;
    }

    /**
     * Return the EOB.getBillablePeriod date range for different FHIR versions
     *
     * @param ben - the EOB
     * @return the billable period
     */
    public static Object getBillablePeriod(IBaseResource ben) {
        if (ben == null) {
            return null;
        }
        return Versions.invokeGetMethod(ben, "getBillablePeriod");
    }

    /**
     * Get start date of a billable period
     *
     * @param ben the EOB object
     * @return the start date
     */
    public static Date getStartDate(IBaseResource ben) {
        Object period = getBillablePeriod(ben);
        if (period == null) {
            return null;
        }
        return (Date) Versions.invokeGetMethod(period, "getStart");
    }

    /**
     * Get end date of a billable period
     *
     * @param ben the EOB object
     * @return the end date
     */
    public static Date getEndDate(IBaseResource ben) {
        Object period = getBillablePeriod(ben);
        if (period == null) {
            return null;
        }
        return (Date) Versions.invokeGetMethod(period, "getEnd");
    }

    /**
     * Returns true if an EOB is part D
     *
     * @param eob - the EOB
     * @return 0 if the EOB is part D
     */
    public static boolean isPartD(IBaseResource eob) {
        if (eob == null) {
            return false;
        }
        if (!eob.fhirType().endsWith("ExplanationOfBenefit")) {
            return false;
        }
        Object c = Versions.invokeGetMethod(eob, "getType");
        List<?> codes = (List<?>) Versions.invokeGetMethod(c, "getCoding");
        return codes.stream()
                .filter(code -> ((String) Versions.invokeGetMethod(code, "getSystem")).endsWith(EOB_TYPE_CODE_SYS))
                .anyMatch(code -> EOB_TYPE_PART_D_CODE_VAL.equalsIgnoreCase((String) Versions.invokeGetMethod(code, "getCode")));
    }
}
